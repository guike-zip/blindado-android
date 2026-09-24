package io.blindado.android.protection

/** Lista fixa de domínios de teste (User Story 3) — anúncios/rastreadores + um domínio comum. */
object TestDomains {
    data class TestDomain(val domain: String, val category: String)

    val ALL: List<TestDomain> = listOf(
        TestDomain("doubleclick.net", "Anúncios/rastreadores"),
        TestDomain("googleadservices.com", "Anúncios/rastreadores"),
        TestDomain("google-analytics.com", "Anúncios/rastreadores"),
        TestDomain("example.com", "Comum"),
    )
}
