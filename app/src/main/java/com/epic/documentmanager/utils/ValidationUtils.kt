package com.epic.documentmanager.utils

import android.util.Patterns
import java.util.regex.Pattern

object ValidationUtils {

    // Email validation
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Password validation
    fun isValidPassword(password: String): Boolean {
        return password.length >= Constants.MIN_PASSWORD_LENGTH
    }

    // Phone number validation (Indonesian format)
    fun isValidPhoneNumber(phone: String): Boolean {
        val pattern = "^(\\+62|62|0)[\\s-]?8[1-9][0-9]{6,9}$"
        return Pattern.compile(pattern).matcher(phone).matches()
    }

    // NIK validation (16 digits)
    fun isValidNIK(nik: String): Boolean {
        return nik.matches(Regex("^[0-9]{16}$"))
    }

    // NPWP validation (15 digits with format XX.XXX.XXX.X-XXX.XXX)
    fun isValidNPWP(npwp: String): Boolean {
        val cleanNPWP = npwp.replace("[^0-9]".toRegex(), "")
        return cleanNPWP.length == 15
    }

    // File size validation (in bytes)
    fun isValidFileSize(sizeInBytes: Long): Boolean {
        val maxSizeInBytes = Constants.MAX_FILE_SIZE_MB * 1024 * 1024
        return sizeInBytes <= maxSizeInBytes
    }

    // File extension validation
    fun isValidImageFile(fileName: String): Boolean {
        val extension = getFileExtension(fileName).lowercase()
        return extension in Constants.ALLOWED_IMAGE_EXTENSIONS
    }

    fun isValidDocumentFile(fileName: String): Boolean {
        val extension = getFileExtension(fileName).lowercase()
        return extension in Constants.ALLOWED_DOCUMENT_EXTENSIONS
    }

    private fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast(".", "")
    }

    // Form validation
    fun validateForm(fields: Map<String, String>): List<String> {
        val errors = mutableListOf<String>()

        fields.forEach { (fieldName, value) ->
            when {
                value.isBlank() -> {
                    errors.add("$fieldName tidak boleh kosong")
                }
                fieldName.lowercase().contains("email") && !isValidEmail(value) -> {
                    errors.add("Format email tidak valid")
                }
                fieldName.lowercase().contains("password") && !isValidPassword(value) -> {
                    errors.add("Password minimal ${Constants.MIN_PASSWORD_LENGTH} karakter")
                }
                fieldName.lowercase().contains("telepon") && !isValidPhoneNumber(value) -> {
                    errors.add("Format nomor telepon tidak valid")
                }
                fieldName.lowercase().contains("nik") && !isValidNIK(value) -> {
                    errors.add("NIK harus 16 digit angka")
                }
                fieldName.lowercase().contains("npwp") && !isValidNPWP(value) -> {
                    errors.add("Format NPWP tidak valid")
                }
            }
        }

        return errors
    }

    // Validate required fields for each document type
    fun validatePembelianRumahForm(data: Map<String, String>): List<String> {
        val errors = mutableListOf<String>()
        val requiredFields = mapOf(
            "Nama" to data["nama"],
            "Alamat KTP" to data["alamatKTP"],
            "NIK" to data["nik"],
            "Nomor Telepon" to data["noTelepon"],
            "Status Pernikahan" to data["statusPernikahan"],
            "Pekerjaan" to data["pekerjaan"],
            "Gaji" to data["gaji"],
            "Tempat Kerja" to data["tempatKerja"],
            "Nama Perumahan" to data["namaPerumahan"],
            "Tipe Rumah" to data["tipeRumah"],
            "Jenis Pembayaran" to data["jenisPembayaran"],
            "Kategori Rumah" to data["tipeRumahKategori"]
        )

        requiredFields.forEach { (fieldName, value) ->
            if (value.isNullOrBlank()) {
                errors.add("$fieldName wajib diisi")
            }
        }

        // Additional validations
        data["nik"]?.let { nik ->
            if (nik.isNotBlank() && !isValidNIK(nik)) {
                errors.add("NIK harus 16 digit angka")
            }
        }

        data["npwp"]?.let { npwp ->
            if (npwp.isNotBlank() && !isValidNPWP(npwp)) {
                errors.add("Format NPWP tidak valid")
            }
        }

        data["noTelepon"]?.let { phone ->
            if (phone.isNotBlank() && !isValidPhoneNumber(phone)) {
                errors.add("Format nomor telepon tidak valid")
            }
        }

        return errors
    }

    fun validateRenovasiRumahForm(data: Map<String, String>): List<String> {
        val errors = mutableListOf<String>()
        val requiredFields = mapOf(
            "Nama" to data["nama"],
            "Alamat" to data["alamat"],
            "Nomor Telepon" to data["noTelepon"],
            "Deskripsi Renovasi" to data["deskripsiRenovasi"]
        )

        requiredFields.forEach { (fieldName, value) ->
            if (value.isNullOrBlank()) {
                errors.add("$fieldName wajib diisi")
            }
        }

        data["noTelepon"]?.let { phone ->
            if (phone.isNotBlank() && !isValidPhoneNumber(phone)) {
                errors.add("Format nomor telepon tidak valid")
            }
        }

        return errors
    }

    fun validatePemasanganACForm(data: Map<String, String>): List<String> {
        val errors = mutableListOf<String>()
        val requiredFields = mapOf(
            "Nama" to data["nama"],
            "Alamat" to data["alamat"],
            "Nomor Telepon" to data["noTelepon"],
            "Jenis AC" to data["jenisAC"],
            "Jumlah Unit" to data["jumlahUnit"]
        )

        requiredFields.forEach { (fieldName, value) ->
            if (value.isNullOrBlank()) {
                errors.add("$fieldName wajib diisi")
            }
        }

        data["noTelepon"]?.let { phone ->
            if (phone.isNotBlank() && !isValidPhoneNumber(phone)) {
                errors.add("Format nomor telepon tidak valid")
            }
        }

        data["jumlahUnit"]?.let { unit ->
            if (unit.isNotBlank()) {
                try {
                    val unitNumber = unit.toInt()
                    if (unitNumber <= 0) {
                        errors.add("Jumlah unit harus lebih dari 0")
                    }
                } catch (e: NumberFormatException) {
                    errors.add("Jumlah unit harus berupa angka")
                }
            }
        }

        return errors
    }

    fun validatePemasanganCCTVForm(data: Map<String, String>): List<String> {
        val errors = mutableListOf<String>()
        val requiredFields = mapOf(
            "Nama" to data["nama"],
            "Alamat" to data["alamat"],
            "Nomor Telepon" to data["noTelepon"],
            "Jumlah Unit" to data["jumlahUnit"]
        )

        requiredFields.forEach { (fieldName, value) ->
            if (value.isNullOrBlank()) {
                errors.add("$fieldName wajib diisi")
            }
        }

        data["noTelepon"]?.let { phone ->
            if (phone.isNotBlank() && !isValidPhoneNumber(phone)) {
                errors.add("Format nomor telepon tidak valid")
            }
        }

        data["jumlahUnit"]?.let { unit ->
            if (unit.isNotBlank()) {
                try {
                    val unitNumber = unit.toInt()
                    if (unitNumber <= 0) {
                        errors.add("Jumlah unit harus lebih dari 0")
                    }
                } catch (e: NumberFormatException) {
                    errors.add("Jumlah unit harus berupa angka")
                }
            }
        }

        return errors
    }

    // Clean and format data
    fun cleanPhoneNumber(phone: String): String {
        var cleaned = phone.replace("[^0-9+]".toRegex(), "")
        if (cleaned.startsWith("0")) {
            cleaned = "+62" + cleaned.substring(1)
        } else if (cleaned.startsWith("62")) {
            cleaned = "+$cleaned"
        }
        return cleaned
    }

    fun formatNPWP(npwp: String): String {
        val cleaned = npwp.replace("[^0-9]".toRegex(), "")
        return if (cleaned.length == 15) {
            "${cleaned.substring(0, 2)}.${cleaned.substring(2, 5)}.${cleaned.substring(5, 8)}.${cleaned.substring(8, 9)}-${cleaned.substring(9, 12)}.${cleaned.substring(12, 15)}"
        } else {
            npwp
        }
    }

    fun formatNIK(nik: String): String {
        return nik.replace("[^0-9]".toRegex(), "")
    }

    // Sanitize input to prevent XSS or injection
    fun sanitizeInput(input: String): String {
        return input.trim()
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
    }
}
//package com.epic.documentmanager.utils
//
//import android.util.Patterns
//
//object ValidationUtils {
//    fun isValidEmail(email: String): Boolean {
//        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
//    }
//
//    fun isValidPassword(password: String): Boolean {
//        return password.length >= 6
//    }
//
//    fun isValidPhone(phone: String): Boolean {
//        return phone.isNotEmpty() && phone.matches(Regex("^[0-9+]{10,15}$"))
//    }
//
//    fun isValidNIK(nik: String): Boolean {
//        return nik.length == 16 && nik.matches(Regex("^[0-9]{16}$"))
//    }
//
//    fun isValidNPWP(npwp: String): Boolean {
//        return npwp.matches(Regex("^[0-9]{2}\\.[0-9]{3}\\.[0-9]{3}\\.[0-9]{1}-[0-9]{3}\\.[0-9]{3}$"))
//    }
//
//    fun validateForm(fields: Map<String, String>): List<String> {
//        val errors = mutableListOf<String>()
//
//        fields.forEach { (key, value) ->
//            when {
//                value.isEmpty() -> errors.add("$key tidak boleh kosong")
//                key == "email" && !isValidEmail(value) -> errors.add("Format email tidak valid")
//                key == "password" && !isValidPassword(value) -> errors.add("Password minimal 6 karakter")
//                key == "noTelepon" && !isValidPhone(value) -> errors.add("Format nomor telepon tidak valid")
//                key == "nik" && !isValidNIK(value) -> errors.add("Format NIK tidak valid (16 digit)")
//                key == "npwp" && !isValidNPWP(value) -> errors.add("Format NPWP tidak valid")
//            }
//        }
//
//        return errors
//    }
//}