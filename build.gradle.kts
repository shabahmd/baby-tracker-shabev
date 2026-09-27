// Nestling — the baby tracker that can't lose your data.
// Single module, no backend, no analytics, no network.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}
