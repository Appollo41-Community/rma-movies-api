package rs.raf.edu.rma.users.auth

import at.favre.lib.crypto.bcrypt.BCrypt

class BcryptPasswordHasher(private val cost: Int = 10) {

    fun hash(plain: String): String {
        require(plain.isNotEmpty()) { "Password must not be empty" }
        return BCrypt.withDefaults().hashToString(cost, plain.toCharArray())
    }

    fun verify(plain: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(plain.toCharArray(), hash).verified
    }
}
