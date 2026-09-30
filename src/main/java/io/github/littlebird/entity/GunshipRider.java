package io.github.littlebird.entity;

/**
 * Marker for a vehicle whose crew is protected while they are strapped in, whichever seat they
 * are in. Every vehicle in the pack implements it, so the damage rule does not have to know which
 * hull a player is sitting in.
 *
 * <p>It also carries the one piece of target state every weapon needs: a hull answers
 * {@link #healthFraction()} with its own remaining integrity, so a new vehicle implements this one
 * method and every gun, rocket and missile that reports a hit shows its bar.
 */
public interface GunshipRider {
    /** Remaining hull integrity as a 0..1 fraction, for the shooter's target readout. */
    float healthFraction();
}
