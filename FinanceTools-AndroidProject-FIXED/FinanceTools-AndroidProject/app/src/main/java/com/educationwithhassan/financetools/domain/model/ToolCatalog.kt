package com.educationwithhassan.financetools.domain.model

object ToolCatalog {
    val allTools: List<FinanceTool> by lazy {
        (TOOLS_CATALOG_PART_1 + TOOLS_CATALOG_PART_2).sortedBy { it.number }
    }

    val byId: Map<String, FinanceTool> by lazy {
        allTools.associateBy { it.id }
    }

    fun forCategory(category: FinanceCategory): List<FinanceTool> =
        allTools.filter { it.category == category }

    fun search(query: String, category: FinanceCategory? = null): List<FinanceTool> {
        val q = query.trim().lowercase()
        return allTools.filter { tool ->
            (category == null || tool.category == category) && (
                q.isEmpty() ||
                    tool.name.lowercase().contains(q) ||
                    tool.shortDescription.lowercase().contains(q) ||
                    tool.category.title.lowercase().contains(q) ||
                    tool.keywords.any { it.lowercase().contains(q) } ||
                    tool.number.toString() == q
                )
        }
    }
}
