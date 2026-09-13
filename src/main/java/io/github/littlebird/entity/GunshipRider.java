package io.github.littlebird.entity;

/**
 * Marker for an aircraft whose crew is protected while they are strapped in, whichever seat
 * they are in. Every vehicle in the pack implements it, so the damage rule does not have to
 * know which airframe a player is sitting in.
 */
public interface GunshipRider {
}
