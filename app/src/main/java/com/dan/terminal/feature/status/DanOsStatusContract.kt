package com.dan.terminal.feature.status

import java.time.Duration
import java.time.Instant

/**
 * Read-only interpretation of a trusted DAN-OS Linux status observation.
 *
 * Transport and JSON parsing deliberately remain outside this contract.
 * Only the handover's allowlisted fields may be supplied. In particular,
 * no private IPs, credentials, hostnames, logs, or backup paths are modeled.
 */
internal data class DanOsLinuxObservation(
    val updated: Instant?,
    val wifi: Boolean?,
    val tailscale: Boolean?,
    val ssh: Boolean?,
    val suspendMasked: Boolean?
)

/** Separate, timestamped Android->Linux probe result, never inferred from Linux status. */
internal data class DanOsPhoneReachability(
    val checkedAt: Instant,
    val reachable: Boolean
)

internal enum class DanOsFreshness { FRESH, STALE, UNKNOWN }
internal enum class DanOsReachability { REACHABLE, UNREACHABLE, UNKNOWN }

/**
 * Flags are null unless the Linux observation is fresh.
 * A fresh Linux record does not imply the Android phone can reach that host.
 */
internal data class DanOsConnectionAssessment(
    val linuxRecord: DanOsFreshness,
    val androidToLinux: DanOsReachability,
    val wifi: Boolean?,
    val tailscale: Boolean?,
    val ssh: Boolean?,
    val suspendMasked: Boolean?
)

internal object DanOsStatusContract {
    const val MAX_AGE_SECONDS = 120L

    private fun freshness(time: Instant?, now: Instant): DanOsFreshness {
        if (time == null) return DanOsFreshness.UNKNOWN
        val age = Duration.between(time, now)
        return when {
            age.isNegative -> DanOsFreshness.UNKNOWN // Future timestamp/clock skew: fail closed.
            age <= Duration.ofSeconds(MAX_AGE_SECONDS) -> DanOsFreshness.FRESH
            else -> DanOsFreshness.STALE
        }
    }

    fun assess(
        linux: DanOsLinuxObservation?,
        phoneProbe: DanOsPhoneReachability?,
        now: Instant
    ): DanOsConnectionAssessment {
        val linuxFreshness = freshness(linux?.updated, now)
        val phoneFreshness = freshness(phoneProbe?.checkedAt, now)
        val androidReachability =
            if (phoneFreshness == DanOsFreshness.FRESH && phoneProbe != null) {
                if (phoneProbe.reachable) DanOsReachability.REACHABLE
                else DanOsReachability.UNREACHABLE
            } else {
                DanOsReachability.UNKNOWN
            }

        // Do not show old or unparseable Linux booleans as current state.
        val available = linux.takeIf { linuxFreshness == DanOsFreshness.FRESH }
        return DanOsConnectionAssessment(
            linuxRecord = linuxFreshness,
            androidToLinux = androidReachability,
            wifi = available?.wifi,
            tailscale = available?.tailscale,
            ssh = available?.ssh,
            suspendMasked = available?.suspendMasked
        )
    }
}
