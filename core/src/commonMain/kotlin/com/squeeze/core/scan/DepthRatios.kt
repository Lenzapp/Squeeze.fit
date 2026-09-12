package com.squeeze.core.scan

/**
 * Depth-to-width ratios derived from the front photograph's own silhouette proportions.
 *
 * A circumference needs two axes. A front view supplies the coronal width; the sagittal
 * depth comes from a side view. With no side view the depth has to be estimated, and these
 * ratios produce that estimate.
 *
 * Rather than using fixed population averages, these ratios are adjusted per-body based on
 * the silhouette's own proportions measured from the same front photograph. The waist-to-hip
 * taper visible in the front silhouette correlates with body shape in the sagittal plane:
 * a rounder, less tapered build has a relatively deeper torso cross-section, while a more
 * tapered, athletic build has a relatively flatter one. Limbs and neck are nearly circular
 * regardless of build and need no adjustment.
 *
 * **Why a front-only scan is still worth taking.** The estimate is wrong for any given
 * person by some amount, but it is wrong by the same amount every time, because a person's
 * build does not change between Tuesday and the following Tuesday. That makes it a
 * systematic offset rather than random scatter -- exactly the error that cancels out when
 * comparing someone against themselves. So a front-only scan tracks change nearly as well
 * as a two-photo scan, and is worse only at the absolute number.
 */
object DepthRatios {

    /**
     * Base depth-to-width ratios for a population-average body shape.
     *
     * The neck is nearly circular. The torso is consistently deeper-than-wide at the hips
     * and flatter at the chest. Limbs are close to round.
     */
    private val baseRatios: Map<ScanSite, Double> = mapOf(
        ScanSite.NECK to 0.95,
        ScanSite.CHEST to 0.70,
        ScanSite.WAIST to 0.72,
        ScanSite.HIP to 0.76,
        ScanSite.THIGH to 0.90,
        ScanSite.ARM to 0.95,
        ScanSite.CALF to 0.90,
    )

    /**
     * How much the measured taper can adjust the base depth ratio, per unit of taper
     * deviation from reference.
     *
     * Only applied to torso sites (chest, waist, hip) where body shape variation has a
     * meaningful effect on sagittal depth. Limbs and neck are left at their base ratios.
     *
     * The maximum adjustment is capped at 10% of the base ratio to prevent extreme values
     * from any single silhouette measurement.
     */
    private const val TAPER_SENSITIVITY = 0.18

    /**
     * Population-average waist-to-hip width ratio, used as the reference point for
     * silhouette-based depth adjustment.
     *
     * An athletic build typically measures 0.78-0.85; a rounder build 0.90-1.05.
     * The reference sits between these, at the midpoint of the population.
     */
    private const val REFERENCE_TAPER = 0.88

    fun depthToWidth(site: ScanSite): Double = baseRatios[site] ?: DEFAULT_RATIO

    /**
     * Estimated sagittal depth for a measured coronal width.
     *
     * Uses fixed ratios when called without silhouette data (legacy/test path).
     */
    fun estimateDepth(site: ScanSite, widthFraction: Double): Double =
        widthFraction * depthToWidth(site)

    /**
     * Estimated sagittal depth for a measured coronal width, adjusted by the front
     * silhouette's measured proportions.
     *
     * The taper (waist width / hip width) is measured directly from the front photograph.
     * A lower taper (narrower waist relative to hip) indicates a more athletic, flatter
     * torso cross-section; a higher taper indicates a rounder, deeper one.
     *
     * @param site the anatomical level being estimated
     * @param widthFraction the coronal width at this site (from the front photo)
     * @param taper waist width / hip width from the same front photograph
     */
    fun estimateDepth(site: ScanSite, widthFraction: Double, taper: Double): Double =
        widthFraction * adjustedRatio(site, taper)

    /**
     * The depth-to-width ratio adjusted by the front silhouette's proportions.
     *
     * Only torso sites (chest, waist, hip) are adjusted. Limbs and neck stay at their
     * base ratio because their cross-section is close to circular regardless of body shape.
     */
    fun adjustedRatio(site: ScanSite, taper: Double): Double {
        val base = baseRatios[site] ?: DEFAULT_RATIO

        // Limbs and neck: nearly circular regardless of build -- no adjustment needed.
        if (site != ScanSite.CHEST && site != ScanSite.WAIST && site != ScanSite.HIP) {
            return base
        }

        // Taper deviation from reference: positive = rounder build, negative = more tapered.
        // The base ratios were calibrated on a population-average body (taper ~0.88).
        // Rounder bodies (higher taper) have relatively deeper torso cross-sections;
        // more tapered (athletic) bodies have relatively flatter ones.
        val taperDeviation = taper - REFERENCE_TAPER
        val adjustment = taperDeviation * TAPER_SENSITIVITY

        // Cap the adjustment at 10% of the base to prevent extreme values from
        // any single silhouette measurement (e.g. loose clothing distorting the waist).
        val maxAdjust = base * 0.10
        val clamped = adjustment.coerceIn(-maxAdjust, maxAdjust)

        return base + clamped
    }

    /**
     * Roughly circular is the safest assumption for an unlisted site.
     */
    private const val DEFAULT_RATIO = 0.85
}
