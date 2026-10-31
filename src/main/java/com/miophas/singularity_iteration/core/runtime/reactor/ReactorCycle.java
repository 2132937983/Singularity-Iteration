// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.reactor;

import java.util.Arrays;
import java.util.Objects;

/**
 * Finite reactor model, aligned with IC2's thermal-then-electrical chamber passes.
 * Has no Minecraft runtime dependency.
 * One call is a 20-tick operation, including cooling when fuel is disabled.
 */
public final class ReactorCycle {
    private ReactorCycle() {}
    /** IC2 resets capacity each cycle; plating raises it when encountered in thermal pass 0. */
    private static final long BASE_MAX_HEAT=10000;
    /** New kinds go after INERT only: saved reactor accident records store the ordinal. */
    public enum Kind { FUEL, CELL, CONDENSATOR, VENT, COMPONENT_VENT, EXCHANGER, REFLECTOR, PLATING, HEATPACK, LITHIUM, ISOTOPE, INERT, NEUTRON_SOURCE,
        STEAM_VENT }
    /**
     * 0.1.10.36: {@code euFactor} / {@code heatFactor} scale the EU and the heat of a fuel rod (1 = uranium; the proton rod 6
     * and 1.5 since 0.1.10.42, with the MOX flag, so its EU also rises with the hull heat; the octuple 12 and 3). The ten-argument
     * constructor keeps both at 1. A rod with the MOX flag doubles its heat in every fluid-mode cycle (the fluid and the steam
     * reactor) above half hull heat.
     */
    public record Profile(Kind kind,int cells,boolean mox,int capacity,int selfCooling,
                          int hullCooling,int adjacentExchange,int hullExchange,int extraHull,int explosionReduction,
                          float euFactor,float heatFactor) {
        public Profile {
            Objects.requireNonNull(kind);
            if (cells<0||cells>4||capacity<0||selfCooling<0||hullCooling<0
                    ||adjacentExchange<0||hullExchange<0||extraHull<0) throw new IllegalArgumentException("Invalid reactor profile");
            if(explosionReduction<0||explosionReduction>100)throw new IllegalArgumentException("Invalid explosion reduction");
            if(kind==Kind.FUEL&&cells!=1&&cells!=2&&cells!=4)throw new IllegalArgumentException("Fuel cells must be 1, 2 or 4");
            if(!(euFactor>0)||!(heatFactor>0)||!Float.isFinite(euFactor)||!Float.isFinite(heatFactor))throw new IllegalArgumentException("Invalid fuel factors");
        }
        public Profile(Kind kind,int cells,boolean mox,int capacity,int selfCooling,
                       int hullCooling,int adjacentExchange,int hullExchange,int extraHull,int explosionReduction){
            this(kind,cells,mox,capacity,selfCooling,hullCooling,adjacentExchange,hullExchange,extraHull,explosionReduction,1.0f,1.0f);
        }
        public static Profile fuel(int cells,boolean mox){return new Profile(Kind.FUEL,cells,mox,0,0,0,0,0,0,0);}
        /** 0.1.10.36: a fuel rod with its own EU and heat factors (the proton rod). */
        public static Profile fuel(int cells,boolean mox,float euFactor,float heatFactor){
            return new Profile(Kind.FUEL,cells,mox,0,0,0,0,0,0,0,euFactor,heatFactor);}
        /**
         * 0.1.10.36: a neutron source (the tritium rod, a D-T neutron emitter): every adjacent fuel rod takes one more pulse
         * (heat and EU), as from a reflector that never wears. It decays by one per cycle (capacity = life, stored = decay) and
         * gives {@code heatPerCycle} of decay heat to the hull.
         */
        public static Profile neutronSource(int life,int heatPerCycle){return new Profile(Kind.NEUTRON_SOURCE,0,false,life,heatPerCycle,0,0,0,0,0);}
        /**
         * 0.1.10.36: an IC2C steam vent. In a steam reactor it turns its heat into steam instead of venting it: it pulls
         * {@code hull} heat from the hull like a vent, then removes up to {@code self} x efficiency, where the efficiency rises
         * from 0 at 100 heat to 1 at full heat, and only as much as the cycle's steam budget allows (water, steam room); heat it
         * cannot turn into steam stays in it. In any other reactor it is an ordinary vent.
         */
        public static Profile steamVent(int capacity,int self,int hull){return thermal(Kind.STEAM_VENT,capacity,self,hull,0,0);}
        public static Profile cell(int capacity){return thermal(Kind.CELL,capacity,0,0,0,0);}
        public static Profile condensator(int capacity){return thermal(Kind.CONDENSATOR,capacity,0,0,0,0);}
        public static Profile vent(int capacity,int self,int hull){return thermal(Kind.VENT,capacity,self,hull,0,0);}
        public static Profile componentVent(){return new Profile(Kind.COMPONENT_VENT,0,false,0,4,0,0,0,0,0);}
        public static Profile exchanger(int capacity,int adjacent,int hull){return thermal(Kind.EXCHANGER,capacity,0,0,adjacent,hull);}
        /** life==0 means infinite durability (as IC2 ItemReactorIridiumReflector). */
        public static Profile reflector(int life){return new Profile(Kind.REFLECTOR,0,false,life,0,0,0,0,0,0);}
        /** explosionReduction is the IC2 influenceExplosion percentage (plating 5 / 10 / 1). 0 means no multiplicative reduction. */
        public static Profile plating(int extra,int explosionReduction){return new Profile(Kind.PLATING,0,false,0,0,0,0,0,extra,explosionReduction);}
        /** Matches IC2 ItemReactorHeatpack(maxPer, heatPer): capacity holds maxPer, selfCooling holds heatPer. */
        public static Profile heatpack(int maxPer,int heatPer){return new Profile(Kind.HEATPACK,0,false,maxPer,heatPer,0,0,0,0,0);}
        /** Matches IC2 ItemReactorLithiumCell: capacity holds the breeding threshold (10000). */
        public static Profile lithium(int threshold){return new Profile(Kind.LITHIUM,0,false,threshold,0,0,0,0,0,0);}
        /**
         * 0.1.10.36: the lithium cell as a real breeder: {@code pulseBonus} more progress per pulse (IC2 experimental: 1 + hull/3000,
         * so it also breeds in a cool reactor) and {@code heatPerPulse} to the hull for every absorbed pulse (lithium-6 + n gives
         * tritium + helium-4 and 4.8 MeV, a little heat). pulseBonus is kept in selfCooling, heatPerPulse in hullCooling.
         */
        public static Profile lithium(int threshold,int pulseBonus,int heatPerPulse){
            return new Profile(Kind.LITHIUM,0,false,threshold,pulseBonus,heatPerPulse,0,0,0,0);}
        /** Matches IC2 ItemReactorDepletedUranium: capacity holds the breeding threshold (10000). selfCooling holds the fixed gain for each pulse (the +1 of IC2). */
        public static Profile isotope(int threshold,int pulseBonus){return new Profile(Kind.ISOTOPE,0,false,threshold,pulseBonus,0,0,0,0,0);}
        private static Profile thermal(Kind kind,int capacity,int self,int hull,int adjacent,int exchange){return new Profile(kind,0,false,capacity,self,hull,adjacent,exchange,0,0);}
        public boolean storesHeat(){return kind==Kind.CELL||kind==Kind.CONDENSATOR||kind==Kind.VENT||kind==Kind.EXCHANGER||kind==Kind.STEAM_VENT;}
        /** Breeding components (IC2 lithium cell / near-depleted isotope rod). They do not store heat and do not exchange heat. Pulses add to their progress. */
        public boolean breeding(){return kind==Kind.LITHIUM||kind==Kind.ISOTOPE;}
    }
    /** Stored is heat for cooling components, wear for reflectors, breeding progress for breeders. Fuel uses remaining cycles. */
    public record Part(Profile profile,int stored,int remaining) {
        public Part {
            Objects.requireNonNull(profile);
            if(stored<0||remaining<0)throw new IllegalArgumentException("Negative component state");
            if(profile.storesHeat()&&stored>profile.capacity())throw new IllegalArgumentException("Component heat exceeds capacity");
            if(profile.kind()==Kind.REFLECTOR&&profile.capacity()>0&&stored>=profile.capacity())throw new IllegalArgumentException("Expired reflector");
            if(profile.breeding()&&profile.capacity()>0&&stored>profile.capacity())throw new IllegalArgumentException("Breeding component exceeds its limit");
        }
        public Part withStored(int amount){return new Part(profile,amount,remaining);}
        public Part withRemaining(int amount){return new Part(profile,stored,amount);}
    }
    /** 0.1.10.36: {@code steamHeat} is the heat the steam vents of a steam reactor turned into steam (0 elsewhere). */
    public record Result(Part[] parts,long hullHeat,long maxHullHeat,long emittedHeat,
                         long generatedHeat,double euPerTick,boolean[] depletedFuel,long steamHeat) {
        public Result {parts=parts.clone();depletedFuel=depletedFuel.clone();}
        public Result(Part[] parts,long hullHeat,long maxHullHeat,long emittedHeat,long generatedHeat,double euPerTick,boolean[] depletedFuel){
            this(parts,hullHeat,maxHullHeat,emittedHeat,generatedHeat,euPerTick,depletedFuel,0);
        }
        @Override public Part[] parts(){return parts.clone();}
        @Override public boolean[] depletedFuel(){return depletedFuel.clone();}
    }
    public static Result step(Part[] input,int columns,long heat,boolean enabled){
        return step(input,columns,heat,enabled,false);
    }
    public static Result step(Part[] input,int columns,long heat,boolean enabled,boolean fluidMode){
        return step(input,columns,heat,enabled,fluidMode,-1);
    }
    /** 0.1.10.36: {@code steamBudget} >= 0 makes it a steam reactor cycle: the steam vents turn at most that much heat into steam. */
    public static Result step(Part[] input,int columns,long heat,boolean enabled,boolean fluidMode,long steamBudget){
        if(input.length!=54||columns<3||columns>9||heat<0)throw new IllegalArgumentException("Invalid reactor state");
        return new Work(input,columns,heat).run(enabled,fluidMode,steamBudget);
    }
    public static int selfPulses(int cells){return switch(cells){case 1->1;case 2->2;case 4->3;default->throw new IllegalArgumentException("Invalid cell count");};}
    public static int fuelHeat(int cells,int pulses){if(pulses<0||pulses>7)throw new IllegalArgumentException("Invalid pulse count");return Math.multiplyExact(2*cells,Math.multiplyExact(pulses,pulses+1));}
    public static double fuelEnergy(int cells,int pulses,boolean mox,long heat,long maxHeat){
        if(heat<0||maxHeat<=0||pulses<0||pulses>7)throw new IllegalArgumentException("Invalid fuel energy inputs");
        float factor=mox?1.0f+((float)heat/(float)maxHeat)*4.0f:1.0f;
        float sum=0;for(int i=0;i<cells*pulses;i++)sum+=factor;
        return sum*5.0f;
    }
    private static final class Work {
        final Part[] parts;final int columns;final boolean[] depleted=new boolean[54];
        long hull,max=BASE_MAX_HEAT,emitted,generated,steamHeat;
        Work(Part[] input,int columns,long heat){this.parts=input.clone();this.columns=columns;this.hull=heat;}
        boolean active(int i){return i>=0&&i<54&&i%9<columns;}
        int[] adjacent(int slot){int[] a=new int[4];int count=0;
            for(int next:new int[]{slot-1,slot+1,slot-9,slot+9})
                if(active(next)&&Math.abs(next/9-slot/9)+Math.abs(next%9-slot%9)==1)a[count++]=next;
            return Arrays.copyOf(a,count);}
        boolean fuel(int slot){Part p=parts[slot];return p!=null&&p.profile().kind()==Kind.FUEL&&p.remaining()>0;}
        /** Same as IC2 checkPulseable: fuel rods, reflectors and breeding components (lithium cell / near-depleted isotope rod) all increase the pulse count. */
        static boolean pulseable(Part part){return part!=null&&(part.profile().kind()==Kind.FUEL&&part.remaining()>0
            ||part.profile().kind()==Kind.REFLECTOR||breeding(part)||emitting(part));}
        static boolean breeding(Part part){return part!=null&&part.profile().breeding()&&part.stored()<part.profile().capacity();}
        /** 0.1.10.36: a neutron source that has not decayed yet. */
        static boolean emitting(Part part){return part!=null&&part.profile().kind()==Kind.NEUTRON_SOURCE&&part.stored()<part.profile().capacity();}
        static boolean canStoreHeat(Part part){return part!=null&&part.profile().storesHeat()&&part.profile().capacity()>0
            &&(part.profile().kind()!=Kind.CONDENSATOR||part.stored()<part.profile().capacity());}
        int pulses(int slot){int n=selfPulses(parts[slot].profile().cells());
            for(int a:adjacent(slot))if(pulseable(parts[a]))n++;
            return n;}
        /** IC2 breeding component: each pulse adds hull heat / 3000 to the progress. A near-depleted isotope rod adds +1 more (ItemReactorDepletedUranium).
         *  At the threshold, the save layer replaces it with the product (tritium fuel rod / re-enriched uranium). */
        void breed(int slot){Part part=parts[slot];int limit=part.profile().capacity();
            long gain=(long)Math.min(Integer.MAX_VALUE,hull/3000)+part.profile().selfCooling();
            parts[slot]=part.withStored((int)Math.min(limit,(long)part.stored()+gain));
            // 0.1.10.36: the lithium reaction's own heat goes to the hull
            if(part.profile().kind()==Kind.LITHIUM&&part.profile().hullCooling()>0){
                generated+=part.profile().hullCooling();hull=Math.addExact(hull,part.profile().hullCooling());
            }}
        Result run(boolean enabled,boolean fluidMode,long steamBudget){
            long budget=steamBudget;
            // Apply component operations in visible row order; fuel expiry is committed after electrical output.
            for(int i=0;i<54;i++){
                if(!active(i)||parts[i]==null)continue;Part p=parts[i];Profile profile=p.profile();
                switch(profile.kind()){
                    case FUEL -> {if(enabled&&fuel(i)){
                        for(int cell=0;cell<profile.cells();cell++){
                            int pulses=selfPulses(profile.cells());
                            for(int a:adjacent(i))if(parts[a]!=null){
                                if(fuel(a))pulses++;
                                else if(parts[a].profile().kind()==Kind.REFLECTOR){
                                    pulses++;Part reflector=parts[a];int limit=reflector.profile().capacity();
                                    if(limit>0)parts[a]=reflector.stored()+1>=limit?null:reflector.withStored(reflector.stored()+1);
                                }else if(breeding(parts[a])){
                                    pulses++;breed(a);
                                }else if(emitting(parts[a])){
                                    pulses++;   // 0.1.10.36: the neutron source never wears per pulse; it decays per cycle
                                }
                            }
                            int amount=2*pulses*(pulses+1);
                            if(profile.heatFactor()!=1.0f)amount=Math.round(amount*profile.heatFactor());
                            if(fluidMode&&profile.mox()&&(float)hull/(float)max>0.5f)amount=Math.multiplyExact(amount,2);
                            generated+=amount;
                            // Each cell distributes immediately, left/right/up/down. Returned heat
                            // (including IC2's negative meltdown remainder) affects the next share.
                            int[] targets=Arrays.stream(adjacent(i)).filter(a->canStoreHeat(parts[a])).toArray();
                            for(int target=0;target<targets.length&&amount>0;target++){
                                int share=amount/(targets.length-target);
                                amount-=share;
                                amount+=alter(targets[target],share);
                            }
                            if(amount>0)hull=Math.addExact(hull,amount);
                        }
                    }}
                    case HEATPACK -> {
                        // IC2 ItemReactorHeatpack(maxPer,heatPer): when the hull heat is below maxPer, add heat to the four adjacent heat-storing components
                        int want=profile.capacity(),heatPer=profile.selfCooling();
                        if(hull<want)for(int a:adjacent(i))if(parts[a]!=null&&parts[a].profile().storesHeat()&&parts[a].profile().capacity()>0){
                            Part target=parts[a];int stored=target.stored();
                            int add=Math.min(heatPer,Math.max(0,want-stored));
                            if(add<=0)continue;
                            int room=target.profile().capacity()-stored;
                            if(room<=0)continue;
                            parts[a]=target.withStored(stored+Math.min(add,room));
                        }
                    }
                    case VENT -> {
                        // IC2 ItemReactorVent: first pull reactorVent heat from the hull into the vent, then self-cool by selfVent.
                        // IC2 alterHeat always returns <= 0 (on overflow it returns max-myHeat+1). Thus its "if > 0 then return"
                        // never runs. When an overflow destroys the component, the hull heat is still removed. SI does the same as IC2; no special case is necessary.
                        int pull=(int)Math.min(hull,profile.hullCooling());hull-=pull;alter(i,pull);
                        // A destroyed vent still completes self-cooling on its old stack.
                        emitted+=parts[i]!=null?remove(i,profile.selfCooling()):Math.min(p.stored(),profile.selfCooling());
                    }
                    case COMPONENT_VENT -> {for(int a:adjacent(i))if(parts[a]!=null&&parts[a].profile().storesHeat()
                            &&parts[a].profile().kind()!=Kind.CONDENSATOR)emitted+=remove(a,profile.selfCooling());}
                    case EXCHANGER -> exchange(i);
                    case PLATING -> max=Math.addExact(max,profile.extraHull());
                    case STEAM_VENT -> {
                        int pull=(int)Math.min(hull,profile.hullCooling());hull-=pull;alter(i,pull);
                        if(parts[i]==null)break;
                        if(budget<0){emitted+=remove(i,profile.selfCooling());break;}   // not a steam reactor: an ordinary vent
                        // 0.1.10.36: efficiency 0 at 100 heat, 1 at full heat (IC2C); what the budget cannot take stays in the vent
                        Part vent=parts[i];
                        double efficiency=Math.clamp((vent.stored()-100.0)/Math.max(1,profile.capacity()-100),0.0,1.0);
                        long take=Math.min(Math.min((long)Math.floor(profile.selfCooling()*efficiency),vent.stored()),budget);
                        if(take>0){remove(i,(int)take);steamHeat+=take;budget-=take;}
                    }
                    case NEUTRON_SOURCE -> {
                        // 0.1.10.36: decay goes on whether the fuel is enabled or not; a little decay heat reaches the hull
                        if(p.stored()<profile.capacity()){
                            parts[i]=p.withStored(Math.min(profile.capacity(),p.stored()+1));
                            if(profile.selfCooling()>0){generated+=profile.selfCooling();hull=Math.addExact(hull,profile.selfCooling());}
                        }
                    }
                    default -> { }
                }
            }
            float units=0;
            if(enabled){
                for(int i=0;i<54;i++)if(active(i)&&fuel(i)){
                    Part p=parts[i];float factor=outputFactor(p);
                    for(int cell=0;cell<p.profile().cells();cell++){
                        for(int pulse=0;pulse<selfPulses(p.profile().cells());pulse++)units+=factor;
                        for(int a:adjacent(i))if(parts[a]!=null){
                            if(fuel(a))units+=outputFactor(parts[a]);
                            // In the electrical pass, IC2 ItemReactorReflector calls back the source fuel rod. This is the same as one more pulse for the source rod
                            // (MOX uses its own factor). Lithium cells / near-depleted isotope rods do not call back. Thus in IC2 they do not increase the EU output.
                            else if(parts[a].profile().kind()==Kind.REFLECTOR)units+=factor;
                            // 0.1.10.36: the neutrons of a neutron source make the rod fission more (one more pulse of EU)
                            else if(emitting(parts[a]))units+=factor;
                        }
                    }
                    depleted[i]=p.remaining()==1;parts[i]=p.withRemaining(p.remaining()-1);
                }
            }
            return new Result(parts,hull,max,emitted,generated,fluidMode?0:units*5.0f,depleted,steamHeat);
        }
        /** Electrical pass follows the entire thermal pass, including cooling and plating. 0.1.10.36: times the rod's EU factor. */
        float outputFactor(Part p){return (p.profile().mox()?1.0f+((float)hull/(float)max)*4.0f:1.0f)*p.profile().euFactor();}
        /**
         * IC2 {@code ItemReactorHeatStorage.alterHeat}: applies a signed heat change to a component. It keeps the IC2 return value rules:
         * · Overflow of a component that is not a condensator: the component melts and is removed. Returns {@code max - myHeat + 1} (negative; the extra heat goes back to the caller).
         * · A negative change that goes below 0: the heat is clamped to 0. Returns the negative remainder.
         * · A condensator only absorbs and never releases (a negative change is returned unchanged). It absorbs as much as it can and returns the rest.
         * · An empty slot returns {@code delta} unchanged.
         */
        int alter(int slot,int delta){Part p=parts[slot];if(p==null)return delta;int capacity=p.profile().capacity();
            if(p.profile().kind()==Kind.CONDENSATOR){
                if(delta<0)return delta;
                int accepted=Math.min(delta,Math.max(0,capacity-p.stored()));parts[slot]=p.withStored(p.stored()+accepted);return delta-accepted;
            }
            long next=(long)p.stored()+delta;
            if(next>capacity){parts[slot]=null;return (int)(capacity-next+1);}
            if(next<0){parts[slot]=p.withStored(0);return (int)next;}
            parts[slot]=p.withStored((int)next);return 0;
        }
        int remove(int slot,int amount){Part p=parts[slot];int accepted=Math.min(p.stored(),Math.max(0,amount));parts[slot]=p.withStored(p.stored()-accepted);return accepted;}
        static double percent(long amount,long capacity){return capacity<=0?0.0D:amount*100.0D/capacity;}
        static double round10(double value){return Math.round(value*10.0D)/10.0D;}
        /**
         * IC2 {@code ItemReactorHeatSwitch.processChamber}: threshold steps on "heat percent of the other + half of own heat percent" set the exchange amount.
         * A comparison rounded to 0.1% sets the direction (if the other is hotter, the direction reverses; if equal, no exchange). At the end, the net change is written back to this component.
         * <p>A neighbour only needs {@code canStoreHeat} (condensators are not excluded). For the exchange with the hull, the threshold steps use {@code switchSide}
         * of original IC2, not {@code switchReactor}. This code copies that behavior.
         */
        void exchange(int slot){
            Part self=parts[slot];if(self==null)return;
            int side=self.profile().adjacentExchange(),hullRate=self.profile().hullExchange();
            int myHeat=0;double mymed=percent(self.stored(),self.profile().capacity());
            if(side>0)for(int a:adjacent(slot)){
                Part other=parts[a];
                if(!canStoreHeat(other))continue;
                // IC2 condensators inherit the public heat accessor returning zero.
                int visibleHeat=other.profile().kind()==Kind.CONDENSATOR?0:other.stored();
                double othermed=percent(visibleHeat,other.profile().capacity()),sum=othermed+mymed/2.0D;
                int add=(int)(other.profile().capacity()/100.0D*sum);
                if(add>side)add=side;
                if(sum<1.0D)add=side/2;
                if(sum<0.75D)add=side/4;
                if(sum<0.5D)add=side/8;
                if(sum<0.25D)add=1;
                if(round10(othermed)>round10(mymed))add=-add;
                else if(round10(othermed)==round10(mymed))add=0;
                myHeat-=add;myHeat+=alter(a,add);
            }
            if(hullRate>0){
                double hullmed=percent(hull,max),sum=hullmed+mymed/2.0D;
                int add=(int)Math.round(max/100.0D*sum);
                if(add>hullRate)add=hullRate;
                if(sum<1.0D)add=side/2;
                if(sum<0.75D)add=side/4;
                if(sum<0.5D)add=side/8;
                if(sum<0.25D)add=1;
                if(round10(hullmed)>round10(mymed))add=-add;
                else if(round10(hullmed)==round10(mymed))add=0;
                myHeat-=add;hull=Math.max(0,hull+add);
            }
            if(parts[slot]!=null)alter(slot,myHeat);
        }
    }
}
