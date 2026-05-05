package com.example.campolibrefutbol

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.math.ceil

sealed class LoginValidationResult {
    data object Success : LoginValidationResult()
    data class InvalidCredentials(val remainingAttempts: Int) : LoginValidationResult()
    data class Locked(val secondsRemaining: Int) : LoginValidationResult()
}

data class AppUser(
    val email: String,
    val createdAt: Long,
    val failedAttempts: Int,
    val lockedUntil: Long,
    val isAdmin: Boolean
)

data class AppReservation(
    val id: Long,
    val canchaNombre: String,
    val canchaTipo: String,
    val fecha: String,
    val hora: String,
    val usuarioEmail: String,
    val precio: String,
    val createdAt: Long
)

class SQLiteUserHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_USERS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_EMAIL TEXT NOT NULL UNIQUE,
                $COL_PASSWORD TEXT,
                $COL_PASSWORD_HASH TEXT,
                $COL_PASSWORD_SALT TEXT,
                $COL_FAILED_ATTEMPTS INTEGER NOT NULL DEFAULT 0,
                $COL_LOCKED_UNTIL INTEGER NOT NULL DEFAULT 0,
                $COL_CREATED_AT INTEGER NOT NULL,
                $COL_IS_ADMIN INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_RESERVATIONS (
                $COL_RESERVATION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_RESERVATION_CANCHA_ID TEXT NOT NULL,
                $COL_RESERVATION_CANCHA_NAME TEXT NOT NULL,
                $COL_RESERVATION_CANCHA_TYPE TEXT NOT NULL,
                $COL_RESERVATION_FECHA TEXT NOT NULL,
                $COL_RESERVATION_HORA TEXT NOT NULL,
                $COL_RESERVATION_USUARIO_EMAIL TEXT NOT NULL,
                $COL_RESERVATION_PRECIO TEXT NOT NULL,
                $COL_RESERVATION_CREATED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )

        ensureDefaultAdminExists(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_PASSWORD_HASH TEXT")
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_PASSWORD_SALT TEXT")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_FAILED_ATTEMPTS INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_LOCKED_UNTIL INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COL_IS_ADMIN INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 5) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_RESERVATIONS (
                    $COL_RESERVATION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_RESERVATION_CANCHA_ID TEXT NOT NULL,
                    $COL_RESERVATION_CANCHA_NAME TEXT NOT NULL,
                    $COL_RESERVATION_CANCHA_TYPE TEXT NOT NULL,
                    $COL_RESERVATION_FECHA TEXT NOT NULL,
                    $COL_RESERVATION_HORA TEXT NOT NULL,
                    $COL_RESERVATION_USUARIO_EMAIL TEXT NOT NULL,
                    $COL_RESERVATION_PRECIO TEXT NOT NULL,
                    $COL_RESERVATION_CREATED_AT INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }

        ensureDefaultAdminExists(db)
    }

    fun ensureDefaultAdminUser() {
        ensureDefaultAdminExists(writableDatabase)
    }

    fun getPasswordPolicyError(password: String): String? {
        if (password.length < 8) return "La contraseña debe tener al menos 8 caracteres"
        if (!password.any { it.isUpperCase() }) return "Incluye al menos una letra mayúscula"
        if (!password.any { it.isDigit() }) return "Incluye al menos un número"
        val hasSymbol = password.any { !it.isLetterOrDigit() }
        if (!hasSymbol) return "Incluye al menos un símbolo (ej: !@#)"
        return null
    }

    fun registerUser(email: String, password: String, isAdmin: Boolean = false): Boolean {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || password.isBlank()) return false
        if (getPasswordPolicyError(password) != null) return false
        val salt = generateSalt()
        val hash = hashPassword(password, salt)

        val values = ContentValues().apply {
            put(COL_EMAIL, normalizedEmail)
            put(COL_PASSWORD, null as String?)
            put(COL_PASSWORD_HASH, hash)
            put(COL_PASSWORD_SALT, salt)
            put(COL_CREATED_AT, System.currentTimeMillis())
            put(COL_IS_ADMIN, if (isAdmin) 1 else 0)
        }

        return writableDatabase.insert(TABLE_USERS, null, values) != -1L
    }

    fun validateUserWithPolicy(email: String, password: String): LoginValidationResult {
        val normalizedEmail = email.trim().lowercase()
        val now = System.currentTimeMillis()

        val cursor = readableDatabase.query(
            TABLE_USERS,
            arrayOf(
                COL_ID,
                COL_PASSWORD,
                COL_PASSWORD_HASH,
                COL_PASSWORD_SALT,
                COL_FAILED_ATTEMPTS,
                COL_LOCKED_UNTIL
            ),
            "$COL_EMAIL = ?",
            arrayOf(normalizedEmail),
            null,
            null,
            null
        )

        cursor.use {
            if (!it.moveToFirst()) {
                return LoginValidationResult.InvalidCredentials(remainingAttempts = MAX_FAILED_ATTEMPTS)
            }

            val id = it.getLong(it.getColumnIndexOrThrow(COL_ID))
            val legacyPassword = it.getString(it.getColumnIndexOrThrow(COL_PASSWORD))
            val storedHash = it.getString(it.getColumnIndexOrThrow(COL_PASSWORD_HASH))
            val storedSalt = it.getString(it.getColumnIndexOrThrow(COL_PASSWORD_SALT))
            val failedAttempts = it.getInt(it.getColumnIndexOrThrow(COL_FAILED_ATTEMPTS))
            val lockedUntil = it.getLong(it.getColumnIndexOrThrow(COL_LOCKED_UNTIL))

            if (lockedUntil > now) {
                val seconds = ceil((lockedUntil - now) / 1000.0).toInt()
                return LoginValidationResult.Locked(secondsRemaining = seconds)
            }

            val isValid = if (!storedHash.isNullOrBlank() && !storedSalt.isNullOrBlank()) {
                verifyPassword(password, storedSalt, storedHash)
            } else {
                !legacyPassword.isNullOrBlank() && legacyPassword == password
            }

            if (isValid) {
                resetLoginProtection(id)
                if (!legacyPassword.isNullOrBlank() && (storedHash.isNullOrBlank() || storedSalt.isNullOrBlank())) {
                    migrateLegacyPassword(id, password)
                }
                return LoginValidationResult.Success
            }

            val newAttempts = failedAttempts + 1
            return if (newAttempts >= MAX_FAILED_ATTEMPTS) {
                lockUser(id, now + LOCKOUT_DURATION_MS)
                LoginValidationResult.Locked(secondsRemaining = (LOCKOUT_DURATION_MS / 1000).toInt())
            } else {
                updateFailedAttempts(id, newAttempts)
                LoginValidationResult.InvalidCredentials(
                    remainingAttempts = MAX_FAILED_ATTEMPTS - newAttempts
                )
            }
        }
    }

    fun userExists(email: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        val cursor = readableDatabase.query(
            TABLE_USERS,
            arrayOf(COL_ID),
            "$COL_EMAIL = ?",
            arrayOf(normalizedEmail),
            null,
            null,
            null
        )

        cursor.use {
            return it.count > 0
        }
    }

    fun isAdminUser(email: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        val cursor = readableDatabase.query(
            TABLE_USERS,
            arrayOf(COL_IS_ADMIN),
            "$COL_EMAIL = ?",
            arrayOf(normalizedEmail),
            null,
            null,
            null
        )

        cursor.use {
            if (!it.moveToFirst()) return false
            return it.getInt(it.getColumnIndexOrThrow(COL_IS_ADMIN)) == 1
        }
    }

    fun updatePassword(email: String, newPassword: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || newPassword.isBlank()) return false
        if (getPasswordPolicyError(newPassword) != null) return false
        val salt = generateSalt()
        val hash = hashPassword(newPassword, salt)

        val values = ContentValues().apply {
            put(COL_PASSWORD, null as String?)
            put(COL_PASSWORD_HASH, hash)
            put(COL_PASSWORD_SALT, salt)
        }

        val updatedRows = writableDatabase.update(
            TABLE_USERS,
            values,
            "$COL_EMAIL = ?",
            arrayOf(normalizedEmail)
        )

        return updatedRows > 0
    }

    fun getAllUsers(): List<AppUser> {
        val users = mutableListOf<AppUser>()
        val cursor = readableDatabase.query(
            TABLE_USERS,
            arrayOf(COL_EMAIL, COL_CREATED_AT, COL_FAILED_ATTEMPTS, COL_LOCKED_UNTIL, COL_IS_ADMIN),
            null,
            null,
            null,
            null,
            "$COL_CREATED_AT DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                users.add(
                    AppUser(
                        email = it.getString(it.getColumnIndexOrThrow(COL_EMAIL)),
                        createdAt = it.getLong(it.getColumnIndexOrThrow(COL_CREATED_AT)),
                        failedAttempts = it.getInt(it.getColumnIndexOrThrow(COL_FAILED_ATTEMPTS)),
                        lockedUntil = it.getLong(it.getColumnIndexOrThrow(COL_LOCKED_UNTIL)),
                        isAdmin = it.getInt(it.getColumnIndexOrThrow(COL_IS_ADMIN)) == 1
                    )
                )
            }
        }

        return users
    }

    fun updateUserAdmin(email: String, isAdmin: Boolean): Boolean {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail == DEFAULT_ADMIN_EMAIL) return false

        if (!isAdmin) {
            val targetIsAdmin = isAdminUser(normalizedEmail)
            if (targetIsAdmin && getAdminUsersCount() <= 1) {
                return false
            }
        }

        val values = ContentValues().apply {
            put(COL_IS_ADMIN, if (isAdmin) 1 else 0)
        }

        val updatedRows = writableDatabase.update(
            TABLE_USERS,
            values,
            "$COL_EMAIL = ?",
            arrayOf(normalizedEmail)
        )

        return updatedRows > 0
    }

    fun deleteUser(email: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail == DEFAULT_ADMIN_EMAIL) return false

        val targetIsAdmin = isAdminUser(normalizedEmail)
        if (targetIsAdmin && getAdminUsersCount() <= 1) {
            return false
        }

        writableDatabase.delete(
            TABLE_RESERVATIONS,
            "$COL_RESERVATION_USUARIO_EMAIL = ?",
            arrayOf(normalizedEmail)
        )

        val deletedRows = writableDatabase.delete(
            TABLE_USERS,
            "$COL_EMAIL = ?",
            arrayOf(normalizedEmail)
        )

        return deletedRows > 0
    }

    private fun getAdminUsersCount(): Int {
        val cursor = readableDatabase.query(
            TABLE_USERS,
            arrayOf("COUNT(*) AS total"),
            "$COL_IS_ADMIN = 1",
            null,
            null,
            null,
            null
        )

        cursor.use {
            if (!it.moveToFirst()) return 0
            return it.getInt(it.getColumnIndexOrThrow("total"))
        }
    }

    fun saveReservation(
        cancha: Cancha,
        fecha: String,
        hora: String,
        usuarioEmail: String?
    ): Long {
        if (isReservationSlotTaken(cancha.id, fecha, hora)) {
            return -1L
        }

        val values = ContentValues().apply {
            put(COL_RESERVATION_CANCHA_ID, cancha.id)
            put(COL_RESERVATION_CANCHA_NAME, cancha.nombre)
            put(COL_RESERVATION_CANCHA_TYPE, cancha.tipo)
            put(COL_RESERVATION_FECHA, fecha)
            put(COL_RESERVATION_HORA, hora)
            put(COL_RESERVATION_USUARIO_EMAIL, usuarioEmail ?: "invitado")
            put(COL_RESERVATION_PRECIO, cancha.precio)
            put(COL_RESERVATION_CREATED_AT, System.currentTimeMillis())
        }

        return writableDatabase.insert(TABLE_RESERVATIONS, null, values)
    }

    fun getReservationsByUser(email: String): List<AppReservation> {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank()) return emptyList()

        val reservations = mutableListOf<AppReservation>()
        val cursor = readableDatabase.query(
            TABLE_RESERVATIONS,
            arrayOf(
                COL_RESERVATION_ID,
                COL_RESERVATION_CANCHA_NAME,
                COL_RESERVATION_CANCHA_TYPE,
                COL_RESERVATION_FECHA,
                COL_RESERVATION_HORA,
                COL_RESERVATION_USUARIO_EMAIL,
                COL_RESERVATION_PRECIO,
                COL_RESERVATION_CREATED_AT
            ),
            "$COL_RESERVATION_USUARIO_EMAIL = ?",
            arrayOf(normalizedEmail),
            null,
            null,
            "$COL_RESERVATION_CREATED_AT DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                reservations.add(
                    AppReservation(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_RESERVATION_ID)),
                        canchaNombre = it.getString(it.getColumnIndexOrThrow(COL_RESERVATION_CANCHA_NAME)),
                        canchaTipo = it.getString(it.getColumnIndexOrThrow(COL_RESERVATION_CANCHA_TYPE)),
                        fecha = it.getString(it.getColumnIndexOrThrow(COL_RESERVATION_FECHA)),
                        hora = it.getString(it.getColumnIndexOrThrow(COL_RESERVATION_HORA)),
                        usuarioEmail = it.getString(it.getColumnIndexOrThrow(COL_RESERVATION_USUARIO_EMAIL)),
                        precio = it.getString(it.getColumnIndexOrThrow(COL_RESERVATION_PRECIO)),
                        createdAt = it.getLong(it.getColumnIndexOrThrow(COL_RESERVATION_CREATED_AT))
                    )
                )
            }
        }

        return reservations
    }

    fun getAllReservations(): List<AppReservation> {
        val reservations = mutableListOf<AppReservation>()
        val cursor = readableDatabase.query(
            TABLE_RESERVATIONS,
            arrayOf(
                COL_RESERVATION_ID,
                COL_RESERVATION_CANCHA_NAME,
                COL_RESERVATION_CANCHA_TYPE,
                COL_RESERVATION_FECHA,
                COL_RESERVATION_HORA,
                COL_RESERVATION_USUARIO_EMAIL,
                COL_RESERVATION_PRECIO,
                COL_RESERVATION_CREATED_AT
            ),
            null,
            null,
            null,
            null,
            "$COL_RESERVATION_CREATED_AT DESC"
        )

        cursor.use {
            while (it.moveToNext()) {
                reservations.add(
                    AppReservation(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_RESERVATION_ID)),
                        canchaNombre = it.getString(it.getColumnIndexOrThrow(COL_RESERVATION_CANCHA_NAME)),
                        canchaTipo = it.getString(it.getColumnIndexOrThrow(COL_RESERVATION_CANCHA_TYPE)),
                        fecha = it.getString(it.getColumnIndexOrThrow(COL_RESERVATION_FECHA)),
                        hora = it.getString(it.getColumnIndexOrThrow(COL_RESERVATION_HORA)),
                        usuarioEmail = it.getString(it.getColumnIndexOrThrow(COL_RESERVATION_USUARIO_EMAIL)),
                        precio = it.getString(it.getColumnIndexOrThrow(COL_RESERVATION_PRECIO)),
                        createdAt = it.getLong(it.getColumnIndexOrThrow(COL_RESERVATION_CREATED_AT))
                    )
                )
            }
        }

        return reservations
    }

    fun deleteReservation(id: Long): Boolean {
        val deletedRows = writableDatabase.delete(
            TABLE_RESERVATIONS,
            "$COL_RESERVATION_ID = ?",
            arrayOf(id.toString())
        )

        return deletedRows > 0
    }

    private fun isReservationSlotTaken(canchaId: String, fecha: String, hora: String): Boolean {
        val cursor = readableDatabase.query(
            TABLE_RESERVATIONS,
            arrayOf(COL_RESERVATION_ID),
            "$COL_RESERVATION_CANCHA_ID = ? AND $COL_RESERVATION_FECHA = ? AND $COL_RESERVATION_HORA = ?",
            arrayOf(canchaId, fecha, hora),
            null,
            null,
            null,
            "1"
        )

        cursor.use {
            return it.moveToFirst()
        }
    }

    private fun ensureDefaultAdminExists(db: SQLiteDatabase) {
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COL_ID),
            "$COL_EMAIL = ?",
            arrayOf(DEFAULT_ADMIN_EMAIL),
            null,
            null,
            null
        )

        cursor.use {
            if (it.moveToFirst()) return
        }

        val salt = generateSalt()
        val hash = hashPassword(DEFAULT_ADMIN_PASSWORD, salt)
        val values = ContentValues().apply {
            put(COL_EMAIL, DEFAULT_ADMIN_EMAIL)
            put(COL_PASSWORD, null as String?)
            put(COL_PASSWORD_HASH, hash)
            put(COL_PASSWORD_SALT, salt)
            put(COL_FAILED_ATTEMPTS, 0)
            put(COL_LOCKED_UNTIL, 0L)
            put(COL_CREATED_AT, System.currentTimeMillis())
            put(COL_IS_ADMIN, 1)
        }
        db.insert(TABLE_USERS, null, values)
    }

    private fun resetLoginProtection(userId: Long) {
        val values = ContentValues().apply {
            put(COL_FAILED_ATTEMPTS, 0)
            put(COL_LOCKED_UNTIL, 0L)
        }
        writableDatabase.update(
            TABLE_USERS,
            values,
            "$COL_ID = ?",
            arrayOf(userId.toString())
        )
    }

    private fun updateFailedAttempts(userId: Long, attempts: Int) {
        val values = ContentValues().apply {
            put(COL_FAILED_ATTEMPTS, attempts)
        }
        writableDatabase.update(
            TABLE_USERS,
            values,
            "$COL_ID = ?",
            arrayOf(userId.toString())
        )
    }

    private fun lockUser(userId: Long, lockUntil: Long) {
        val values = ContentValues().apply {
            put(COL_FAILED_ATTEMPTS, 0)
            put(COL_LOCKED_UNTIL, lockUntil)
        }
        writableDatabase.update(
            TABLE_USERS,
            values,
            "$COL_ID = ?",
            arrayOf(userId.toString())
        )
    }

    private fun migrateLegacyPassword(userId: Long, plainPassword: String) {
        val salt = generateSalt()
        val hash = hashPassword(plainPassword, salt)

        val values = ContentValues().apply {
            put(COL_PASSWORD, null as String?)
            put(COL_PASSWORD_HASH, hash)
            put(COL_PASSWORD_SALT, salt)
        }

        writableDatabase.update(
            TABLE_USERS,
            values,
            "$COL_ID = ?",
            arrayOf(userId.toString())
        )
    }

    private fun generateSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    private fun hashPassword(password: String, saltBase64: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val saltBytes = Base64.decode(saltBase64, Base64.NO_WRAP)
        md.update(saltBytes)
        val hash = md.digest(password.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    private fun verifyPassword(password: String, saltBase64: String, expectedHashBase64: String): Boolean {
        val calculated = hashPassword(password, saltBase64)
        return MessageDigest.isEqual(
            calculated.toByteArray(Charsets.UTF_8),
            expectedHashBase64.toByteArray(Charsets.UTF_8)
        )
    }

    companion object {
        private const val DATABASE_NAME = "campolibre_users.db"
        private const val DATABASE_VERSION = 5
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L

        const val DEFAULT_ADMIN_EMAIL = "admin@campolibre.com"
        const val DEFAULT_ADMIN_PASSWORD = "Admin123!"

        private const val TABLE_USERS = "users"
        private const val COL_ID = "id"
        private const val COL_EMAIL = "email"
        private const val COL_PASSWORD = "password"
        private const val COL_PASSWORD_HASH = "password_hash"
        private const val COL_PASSWORD_SALT = "password_salt"
        private const val COL_FAILED_ATTEMPTS = "failed_attempts"
        private const val COL_LOCKED_UNTIL = "locked_until"
        private const val COL_CREATED_AT = "created_at"
        private const val COL_IS_ADMIN = "is_admin"

        private const val TABLE_RESERVATIONS = "reservations"
        private const val COL_RESERVATION_ID = "id"
        private const val COL_RESERVATION_CANCHA_ID = "cancha_id"
        private const val COL_RESERVATION_CANCHA_NAME = "cancha_nombre"
        private const val COL_RESERVATION_CANCHA_TYPE = "cancha_tipo"
        private const val COL_RESERVATION_FECHA = "fecha"
        private const val COL_RESERVATION_HORA = "hora"
        private const val COL_RESERVATION_USUARIO_EMAIL = "usuario_email"
        private const val COL_RESERVATION_PRECIO = "precio"
        private const val COL_RESERVATION_CREATED_AT = "created_at"
    }
}
