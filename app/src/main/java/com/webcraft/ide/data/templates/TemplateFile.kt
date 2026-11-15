package com.webcraft.ide.data.templates

data class TemplateFile(val path: String, val content: String)
data class ProjectTemplate(val id: String, val name: String, val description: String, val files: List<TemplateFile>)
