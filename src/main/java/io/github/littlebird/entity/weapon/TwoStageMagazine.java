package io.github.littlebird.entity.weapon;

/**
 * Two stage ammunition: what is loaded and ready to fire, and what is held in reserve.
 *
 * Firing only ever drains the ready rounds. Reloading is the crew's job - the reload key moves
 * rounds from the reserve into the ready position (2 s for a 19 tube rocket bay, 5 s for a
 * single missile rail) - while a slower automatic resupply tops the reserve back up
 * (15 s for a full rocket bay, 10 s for one missile). Pure rules: no Minecraft classes.
 */
public final class TwoStageMagazine {

    /** readyCapacity + reserveCapacity is the whole load; times are in ticks. */
    public record Spec(int readyCapacity, int reserveCapacity, int reloadTicks,
                       int resupplyTicks, int resupplyBatch) {
        public Spec {
            readyCapacity = Math.max(0, readyCapacity);
            reserveCapacity = Math.max(0, reserveCapacity);
            reloadTicks = Math.max(0, reloadTicks);
            resupplyTicks = Math.max(0, resupplyTicks);
            resupplyBatch = Math.max(1, resupplyBatch);
        }

        /**
         * The same load with different clocks: the capacities and the batch size are the
         * weapon's design, while the reload and resupply times are balance values the config
         * owns. Shared by every aircraft whose magazines are configurable.
         */
        public Spec withTimings(int newReloadTicks, int newResupplyTicks) {
            return new Spec(readyCapacity, reserveCapacity, newReloadTicks, newResupplyTicks,
                    resupplyBatch);
        }
    }

    /** reloadTicks/resupplyTicks count down to zero; zero means idle. */
    public record State(int ready, int reserve, int reloadTicks, int resupplyTicks) {
        public int total() {
            return ready + reserve;
        }
    }

    /**
     * 19 ready + 95 reserve and a 2 s reload. There is no automatic resupply any more: the
     * reserve only grows when the aircraft is parked next to an ammunition crate, so these
     * specs carry a zero resupply clock on purpose.
     */
    public static Spec rocketBay() {
        return new Spec(19, 95, 40, 0, 19);
    }

    /** 1 in the rail + 5 in reserve, 5 s reload, again with no self resupply. */
    public static Spec missileRail() {
        return new Spec(1, 5, 100, 0, 1);
    }

    /** Tops the reserve up from a crate, never past its capacity. */
    public static State supply(State state, Spec spec, int rounds) {
        int reserve = Math.min(spec.reserveCapacity(), state.reserve() + Math.max(0, rounds));
        return new State(state.ready(), reserve, state.reloadTicks(), state.resupplyTicks());
    }

    public static State initial(Spec spec) {
        return new State(spec.readyCapacity(), spec.reserveCapacity(), 0, 0);
    }

    /** True when the reload key would actually do something. */
    public static boolean canReload(State state, Spec spec) {
        return state.reloadTicks() <= 0
                && state.reserve() > 0
                && state.ready() < spec.readyCapacity();
    }

    /** One round leaves the ready position. Firing an empty magazine changes nothing. */
    public static State fire(State state, Spec spec) {
        if (state.ready() <= 0) {
            return state;
        }
        return new State(state.ready() - 1, state.reserve(), state.reloadTicks(),
                state.resupplyTicks());
    }

    /** The reload key: moves reserve rounds into the ready position after reloadTicks. */
    public static State startReload(State state, Spec spec) {
        if (!canReload(state, spec)) {
            return state;
        }
        return new State(state.ready(), state.reserve(), spec.reloadTicks(),
                state.resupplyTicks());
    }

    /** Advances both timers by one tick. */
    public static State tick(State state, Spec spec) {
        int ready = state.ready();
        int reserve = state.reserve();
        int reload = Math.max(0, state.reloadTicks());
        int resupply = Math.max(0, state.resupplyTicks());

        if (reload > 0) {
            reload--;
            if (reload == 0) {
                int moved = Math.min(spec.readyCapacity() - ready, reserve);
                ready += moved;
                reserve -= moved;
            }
        }

        if (spec.resupplyTicks() <= 0 || reserve >= spec.reserveCapacity()) {
            resupply = 0;
        } else {
            if (resupply <= 0) {
                resupply = spec.resupplyTicks();
            }
            // The tick that starts the timer is already spent, so a batch lands exactly
            // resupplyTicks after the reserve drops below capacity.
            resupply--;
            if (resupply == 0) {
                reserve = Math.min(spec.reserveCapacity(), reserve + spec.resupplyBatch());
                resupply = spec.resupplyTicks();
            }
        }

        return new State(ready, reserve, reload, resupply);
    }
}
