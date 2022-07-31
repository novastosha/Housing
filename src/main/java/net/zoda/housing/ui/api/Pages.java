package net.zoda.housing.ui.api;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class Pages<T> implements Iterable<List<T>> {

    private final List<T> rawObjectsList;
    private final List<T> sortedDone = new ArrayList<>();
    private final int split;
    private final HashMap<Integer, List<T>> sorted;
    private int globalIndex = 0;


    /**
     *
     * Creates a hashmap of sorted things, usually takes about 1 ms to create.
     *
     * @param objects List of things to sort
     */
    public Pages(List<T> objects, int split){

        this.rawObjectsList = objects;
        this.sorted = new HashMap<>();
        this.split = split;
        if(rawObjectsList.size() == 0){
            throw new IllegalStateException("Size of the array cannot be 0");
        }

        loop();
    }

    private void loop() {
        List<T> current = new ArrayList<>();

        int index = 1;
        for(T value : rawObjectsList) {

            if(!sortedDone.contains(value)) {

                sortedDone.add(value);

                current.add(value);

                if(index == split){
                    sorted.put(globalIndex,current);
                    globalIndex++;

                    if(sortedDone.size() != rawObjectsList.size()) {
                        loop();
                    }
                    return;
                }

                sorted.put(globalIndex,current);

                index++;
            }

        }

    }

    public HashMap<Integer, List<T>> getSorted() {
        return sorted;
    }

    public List<T> getRawObjectsList() {
        return rawObjectsList;
    }

    public int getSplit() {
        return split;
    }

    @Override
    public Iterator<List<T>> iterator() {
        return getSorted().values().iterator();
    }
}