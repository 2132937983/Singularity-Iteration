package com.singularity_iteration.mio_icif.uu;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * 配方转换数据类
 * 与 IC2 1.12.2 原版 RecipeTransformation 对应
 */
public class RecipeTransformation {
    public final double transformCost;
    public List<List<LeanItemStack>> inputs;
    public List<LeanItemStack> outputs;

    public RecipeTransformation(double transformCost, List<List<LeanItemStack>> inputs, LeanItemStack... outputs) {
        this(transformCost, inputs, List.of(outputs));
    }

    public RecipeTransformation(double transformCost, List<List<LeanItemStack>> inputs, List<LeanItemStack> outputs) {
        this.transformCost = transformCost;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    /**
     * 合并相同的输入和输出
     */
    protected void merge() {
        List<List<LeanItemStack>> cleanInputs = new ArrayList<>();

        for (List<LeanItemStack> inputList : this.inputs) {
            boolean found = false;
            for (ListIterator<List<LeanItemStack>> it = cleanInputs.listIterator(); it.hasNext(); ) {
                List<LeanItemStack> cleanInputList = it.next();
                cleanInputList = mergeEqualLists(inputList, cleanInputList);
                if (cleanInputList != null) {
                    found = true;
                    it.set(cleanInputList);
                    break;
                }
            }
            if (!found) cleanInputs.add(inputList);
        }

        this.inputs = cleanInputs;

        List<LeanItemStack> cleanOutputs = new ArrayList<>();
        for (LeanItemStack output : this.outputs) {
            boolean found = false;
            for (ListIterator<LeanItemStack> it = cleanOutputs.listIterator(); it.hasNext(); ) {
                LeanItemStack stack = it.next();
                if (output.hasSameItem(stack)) {
                    found = true;
                    it.set(stack.copyWithSize(stack.getSize() + output.getSize()));
                    break;
                }
            }
            if (!found) cleanOutputs.add(output);
        }
        this.outputs = cleanOutputs;
    }

    @Override
    public String toString() {
        return "{ " + this.transformCost + " + " + this.inputs + " -> " + this.outputs + " }";
    }

    private List<LeanItemStack> mergeEqualLists(List<LeanItemStack> listA, List<LeanItemStack> listB) {
        if (listA.size() != listB.size()) return null;

        List<LeanItemStack> ret = new ArrayList<>(listA.size());
        List<LeanItemStack> listBCopy = new LinkedList<>(listB);

        for (LeanItemStack a : listA) {
            boolean found = false;
            for (Iterator<LeanItemStack> it = listBCopy.iterator(); it.hasNext(); ) {
                LeanItemStack b = it.next();
                if (a.hasSameItem(b)) {
                    found = true;
                    ret.add(a.copyWithSize(a.getSize() + b.getSize()));
                    it.remove();
                    break;
                }
            }
            if (!found) return null;
        }
        return ret;
    }
}