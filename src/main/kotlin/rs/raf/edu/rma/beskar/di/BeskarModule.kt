package rs.raf.edu.rma.beskar.di

import org.jetbrains.exposed.sql.Database
import org.koin.core.qualifier.named
import org.koin.dsl.module
import rs.raf.edu.rma.beskar.repository.BeskarRepository

val beskar = module {
    single(named("beskarDb")) {
        val dbUrl = System.getenv("BESKAR_DATABASE_URL") ?: "data/beskar.db"
        Database.connect("jdbc:sqlite:$dbUrl", driver = "org.sqlite.JDBC")
    }

    single {
        BeskarRepository(database = get(named("beskarDb")))
    }
}
