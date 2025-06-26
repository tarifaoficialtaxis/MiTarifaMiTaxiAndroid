package com.mitarifamitaxi.taximetrousuario.models

data class CountryArea (
    val country: String? = null,
    val features: List<Feature>? = null,
    val type: String? = null
)

data class Feature (
    val type: FeatureType? = null,
    val properties: Properties? = null,
    val geometry: Geometry? = null,
    val id: Long? = null
)

data class Geometry (
    val coordinates: List<List<List<Double>>>? = null,
    val type: GeometryType? = null
)

enum class GeometryType {
    Polygon
}

data class Properties (
    val id: String? = null,
    val name: String? = null,
)

enum class FeatureType {
    Feature
}