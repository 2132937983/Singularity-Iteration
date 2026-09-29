// SPDX-License-Identifier: Apache-2.0
package com.miophas.singularity_iteration.core.runtime.processing;

import net.minecraft.world.inventory.ContainerData;

/** Vanilla menu property packets carry signed 16-bit values. Reassemble unsigned limbs. */
public final class PatternMenuData {
    public static final int COUNT=14,ENERGY=0,CAPACITY=2,INDEX=4,SIZE=5,UU=6,EU=10;
    private PatternMenuData(){}
    public static int word(long value,int index){if(index<0||index>3)throw new IndexOutOfBoundsException(index);return (int)(value>>>(16*index))&0xFFFF;}
    public static long read(ContainerData data,int first,int words) {
        if(words<1||words>4)throw new IllegalArgumentException("word count");long value=0;
        for(int i=0;i<words;i++)value|=(data.get(first+i)&0xFFFFL)<<(16*i);
        return value;
    }
    public static void write(ContainerData data,int first,int words,long value) {
        if(words<1||words>4)throw new IllegalArgumentException("word count");
        for(int i=0;i<words;i++)data.set(first+i,word(value,i));
    }
}
