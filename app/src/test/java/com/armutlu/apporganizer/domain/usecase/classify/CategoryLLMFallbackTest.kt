package com.armutlu.apporganizer.domain.usecase.classify

import com.armutlu.apporganizer.domain.models.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategoryLLMFallbackTest {

    @Test
    fun `normalizes LLM wire category to Room id`() {
        assertEquals(
            Category.CAT_GAMES,
            CategoryLLMFallback.normalizeCategoryId("CAT_GAMES"),
        )
        assertEquals(
            Category.CAT_PHOTOGRAPHY,
            CategoryLLMFallback.normalizeCategoryId("CAT_PHOTO"),
        )
        assertEquals(
            Category.CAT_UTILITIES,
            CategoryLLMFallback.normalizeCategoryId("CAT_TOOLS"),
        )
    }

    @Test
    fun `normalization is case and whitespace tolerant`() {
        assertEquals(
            Category.CAT_SOCIAL,
            CategoryLLMFallback.normalizeCategoryId("  cat_social "),
        )
    }

    @Test
    fun `unknown LLM category is rejected`() {
        assertNull(CategoryLLMFallback.normalizeCategoryId("CAT_NOT_A_REAL_CATEGORY"))
        assertNull(CategoryLLMFallback.normalizeCategoryId(null))
    }
}
