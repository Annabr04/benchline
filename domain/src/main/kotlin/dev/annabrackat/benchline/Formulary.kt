package dev.annabrackat.benchline

/** Training formulary. NDCs are synthetic. Not a drug database. */
object Formulary {
    val drugs: List<Drug> = listOf(
        Drug("lisinopril", "9901-1001-01", "09901100101", "Lisinopril", "10 mg tablet"),
        Drug("metformin", "99002-040-10", "99002004010", "Metformin", "500 mg tablet"),
        Drug("atorvastatin", "99003-2001-1", "99003200101", "Atorvastatin", "20 mg tablet"),
        Drug("hydroxyzine", "99004-025-01", "99004002501", "hydrOXYzine", "25 mg tablet", "hydralazine"),
        Drug("hydralazine", "99005-025-01", "99005002501", "hydrALAZINE", "25 mg tablet", "hydroxyzine"),
        Drug("prednisone", "99006-010-01", "99006001001", "predniSONE", "10 mg tablet", "prednisolone"),
        Drug("prednisolone", "99007-010-01", "99007001001", "prednisoLONE", "10 mg tablet", "prednisone"),
        Drug("trazodone", "99008-050-01", "99008005001", "traZODone", "50 mg tablet", "tramadol"),
        Drug("tramadol", "99009-050-01", "99009005001", "traMADol", "50 mg tablet", "trazodone"),
    )

    fun byId(id: String): Drug? = drugs.find { it.id == id }

    fun partnerOf(drug: Drug): Drug? = drug.lasaWith?.let(::byId)
}
