package com.nevis.search.api

import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/** Lets `?type=client` and `?type=CLIENT` both work. */
@Configuration
class EnumConverters : WebMvcConfigurer {
    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(SearchTypeConverter)
    }

    object SearchTypeConverter : Converter<String, SearchType> {
        override fun convert(source: String): SearchType = SearchType.valueOf(source.trim().uppercase())
    }
}
