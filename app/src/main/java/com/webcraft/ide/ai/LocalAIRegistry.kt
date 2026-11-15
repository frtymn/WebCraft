package com.webcraft.ide.ai

/**
 * Registry that owns the currently active LocalAIProvider.
 *
 * Today only the rules-based engine is registered. To add an on-device
 * LLM in the future, implement [LocalAIProvider] and register it here —
 * the UI will pick it up automatically.
 */
object LocalAIRegistry {
    private val providers = mutableMapOf<String, LocalAIProvider>()
    @Volatile private var active: LocalAIProvider? = null

    init {
        register(RulesBasedProvider())
    }

    fun register(provider: LocalAIProvider) {
        providers[provider.displayName] = provider
        if (active == null) active = provider
    }

    fun active(): LocalAIProvider = active ?: RulesBasedProvider()

    fun setActive(displayName: String): Boolean {
        val p = providers[displayName] ?: return false
        active = p
        return true
    }

    fun all(): List<LocalAIProvider> = providers.values.toList()
}
