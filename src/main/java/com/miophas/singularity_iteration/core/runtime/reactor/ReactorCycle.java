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
    public enum Kind { FUEL, CELL, CONDENSATOR, VENT, COMPONENT_VENT, EXCHANGER, REFLECTOR, PLATING, HEATPACK, LITHIUM, ISOTOPE, INERT }
    public record Profile(Kind kind,int cells,boolean mox,int capacity,int selfCooling,
                          int hullCooling,int adjacentExchange,int hullExchange,int extraHull,int explosionReduction) {
        public Profile {
            Objects.requireNonNull(kind);
            if (cells<0||cells>4||capacity<0||selfCooling<0||hullCooling<0
                    ||adjacentExchange<0||hullExchange<0||extraHull<0) throw new IllegalArgumentException("Invalid reactor profile");
            if(explosionReduction<0||explosionReduction>100)throw new IllegalArgumentException("Invalid explosion reduction");
            if(kind==Kind.FUEL&&cells!=1&&cells!=2&&cells!=4)throw new IllegalArgumentException("Fuel cells must be 1, 2 or 4");
        }
        public static Profile fuel(int cells,boolean mox){return new Profile(Kind.FUEL,cells,mox,0,0,0,0,0,0,0);}
        public static Profile cell(int capacity){return thermal(Kind.CELL,capacity,0,0,0,0);}
        public static Profile condensator(int capacity){return thermal(Kind.CONDENSATOR,capacity,0,0,0,0);}
        public static Profile vent(int capacity,int self,int hull){return thermal(Kind.VENT,capacity,self,hull,0,0);}
        public static Profile componentVent(){return new Profile(Kind.COMPONENT_VENT,0,false,0,4,0,0,0,0,0);}
        public static Profile exchanger(int capacity,int adjacent,int hull){return thermal(Kind.EXCHANGER,capacity,0,0,adjacent,hull);}
        /** life==0 表示无限耐久（对应 IC2 ItemReactorIridiumReflector）。 */
        public static Profile reflector(int life){return new Profile(Kind.REFLECTOR,0,false,life,0,0,0,0,0,0);}
        /** explosionReduction 为 IC2 influenceExplosion 的百分比（隔板 5 / 10 / 1），0 表示不参与乘法减免。 */
        public static Profile plating(int extra,int explosionReduction){return new Profile(Kind.PLATING,0,false,0,0,0,0,0,extra,explosionReduction);}
        /** 对应 IC2 ItemReactorHeatpack(maxPer, heatPer)：capacity 存 maxPer，selfCooling 存 heatPer。 */
        public static Profile heatpack(int maxPer,int heatPer){return new Profile(Kind.HEATPACK,0,false,maxPer,heatPer,0,0,0,0,0);}
        /** 对应 IC2 ItemReactorLithiumCell：capacity 存增殖阈值（10000）。 */
        public static Profile lithium(int threshold){return new Profile(Kind.LITHIUM,0,false,threshold,0,0,0,0,0,0);}
        /** 对应 IC2 ItemReactorDepletedUranium：capacity 存增殖阈值（10000），selfCooling 存每个脉冲的固定增量（IC2 的 +1）。 */
        public static Profile isotope(int threshold,int pulseBonus){return new Profile(Kind.ISOTOPE,0,false,threshold,pulseBonus,0,0,0,0,0);}
        private static Profile thermal(Kind kind,int capacity,int self,int hull,int adjacent,int exchange){return new Profile(kind,0,false,capacity,self,hull,adjacent,exchange,0,0);}
        public boolean storesHeat(){return kind==Kind.CELL||kind==Kind.CONDENSATOR||kind==Kind.VENT||kind==Kind.EXCHANGER;}
        /** 增殖元件（IC2 锂单元 / 近衰变同位素棒）：不储热、不参与换热，受脉冲累积进度。 */
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
    public record Result(Part[] parts,long hullHeat,long maxHullHeat,long emittedHeat,
                         long generatedHeat,double euPerTick,boolean[] depletedFuel) {
        public Result {parts=parts.clone();depletedFuel=depletedFuel.clone();}
        @Override public Part[] parts(){return parts.clone();}
        @Override public boolean[] depletedFuel(){return depletedFuel.clone();}
    }
    public static Result step(Part[] input,int columns,long heat,boolean enabled){
        return step(input,columns,heat,enabled,false);
    }
    public static Result step(Part[] input,int columns,long heat,boolean enabled,boolean fluidMode){
        if(input.length!=54||columns<3||columns>9||heat<0)throw new IllegalArgumentException("Invalid reactor state");
        return new Work(input,columns,heat).run(enabled,fluidMode);
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
        long hull,max=BASE_MAX_HEAT,emitted,generated;
        Work(Part[] input,int columns,long heat){this.parts=input.clone();this.columns=columns;this.hull=heat;}
        boolean active(int i){return i>=0&&i<54&&i%9<columns;}
        int[] adjacent(int slot){int[] a=new int[4];int count=0;
            for(int next:new int[]{slot-1,slot+1,slot-9,slot+9})
                if(active(next)&&Math.abs(next/9-slot/9)+Math.abs(next%9-slot%9)==1)a[count++]=next;
            return Arrays.copyOf(a,count);}
        boolean fuel(int slot){Part p=parts[slot];return p!=null&&p.profile().kind()==Kind.FUEL&&p.remaining()>0;}
        /** 与 IC2 checkPulseable 一致：燃料棒、反射器与增殖元件（锂单元 / 近衰变同位素棒）都会放大脉冲数。 */
        static boolean pulseable(Part part){return part!=null&&(part.profile().kind()==Kind.FUEL&&part.remaining()>0
            ||part.profile().kind()==Kind.REFLECTOR||breeding(part));}
        static boolean breeding(Part part){return part!=null&&part.profile().breeding()&&part.stored()<part.profile().capacity();}
        static boolean canStoreHeat(Part part){return part!=null&&part.profile().storesHeat()&&part.profile().capacity()>0
            &&(part.profile().kind()!=Kind.CONDENSATOR||part.stored()<part.profile().capacity());}
        int pulses(int slot){int n=selfPulses(parts[slot].profile().cells());
            for(int a:adjacent(slot))if(pulseable(parts[a]))n++;
            return n;}
        /** IC2 增殖元件：每收到一个脉冲按 堆温/3000 累积；近衰变同位素棒另加 +1（ItemReactorDepletedUranium），
         *  达到阈值后由存档层换成产物（氚燃料棒 / 重新浓缩的铀）。 */
        void breed(int slot){Part part=parts[slot];int limit=part.profile().capacity();
            long gain=(long)Math.min(Integer.MAX_VALUE,hull/3000)+part.profile().selfCooling();
            parts[slot]=part.withStored((int)Math.min(limit,(long)part.stored()+gain));}
        Result run(boolean enabled,boolean fluidMode){
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
                                }
                            }
                            int amount=2*pulses*(pulses+1);
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
                        // IC2 ItemReactorHeatpack(maxPer,heatPer)：堆温低于 maxPer 时，向四邻可储热元件注入热量
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
                        // IC2 ItemReactorVent：先按 reactorVent 从堆温吸热进自身，再按 selfVent 自冷。
                        // IC2 的 alterHeat 返回值恒 <= 0（溢出时返回 max-myHeat+1），所以那句 `> 0 则 return`
                        // 永远不会触发 —— 溢出摧毁元件时堆温同样照扣，SI 此处与 IC2 一致，无需特判。
                        int pull=(int)Math.min(hull,profile.hullCooling());hull-=pull;alter(i,pull);
                        // A destroyed vent still completes self-cooling on its old stack.
                        emitted+=parts[i]!=null?remove(i,profile.selfCooling()):Math.min(p.stored(),profile.selfCooling());
                    }
                    case COMPONENT_VENT -> {for(int a:adjacent(i))if(parts[a]!=null&&parts[a].profile().storesHeat()
                            &&parts[a].profile().kind()!=Kind.CONDENSATOR)emitted+=remove(a,profile.selfCooling());}
                    case EXCHANGER -> exchange(i);
                    case PLATING -> max=Math.addExact(max,profile.extraHull());
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
                            // IC2 ItemReactorReflector 在电力阶段会回调源燃料棒，等价于源棒再多收一个脉冲
                            // （MOX 用它自己的系数）。锂单元 / 近衰变同位素棒不回调，所以 IC2 里它们不放大 EU 输出。
                            else if(parts[a].profile().kind()==Kind.REFLECTOR)units+=factor;
                        }
                    }
                    depleted[i]=p.remaining()==1;parts[i]=p.withRemaining(p.remaining()-1);
                }
            }
            return new Result(parts,hull,max,emitted,generated,fluidMode?0:units*5.0f,depleted);
        }
        /** Electrical pass follows the entire thermal pass, including cooling and plating. */
        float outputFactor(Part p){return p.profile().mox()?1.0f+((float)hull/(float)max)*4.0f:1.0f;}
        /**
         * IC2 {@code ItemReactorHeatStorage.alterHeat}：对元件施加带符号的热量变化，并沿用其返回值语义：
         * · 非冷凝器溢出 → 元件熔毁消失，返回 {@code max - myHeat + 1}（负数，多出的热量退回调用者）；
         * · 负向把热量压到 0 以下 → 夹到 0，返回负的残余量；
         * · 冷凝器只吸不放（负向原样返回），能吸多少吸多少、余量全部退回。
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
         * IC2 {@code ItemReactorHeatSwitch.processChamber}：交换量由"对方热量百分比 + 自身一半百分比"经阈值档位决定，
         * 方向由 0.1% 取整后的比较决定（对方更热则反向、持平则不交换），最后把净变化写回自身。
         * <p>邻位只需 {@code canStoreHeat}（不排除冷凝器）；与堆交换时的阈值档位沿用 IC2 原版的 {@code switchSide}
         * 而非 {@code switchReactor}，此处照搬该行为。
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
