package io.ferdon.statespace;

import com.google.common.collect.Multiset;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.javatuples.Pair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.*;

import io.ferdon.statespace.Petrinet.Token;


class StateSpace {

    class State {
        Map<Integer, Multiset<Token>> state;

        State(Map<Integer, Multiset<Token>> x) {
            state = x;
        }

        Multiset<Token> get(int placeID) {
            return state.get(placeID);
        }

        Set<Integer> getKeySet() {
            return state.keySet();
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder();
            for (int i : state.keySet()) {
                s.append(i);
                s.append("->[");
                for (Token token : state.get(i)) {
                    s.append(token.toString());
                }
                s.append("]");
            }
            return s.toString();
        }
    }

    private long P;
    private int numState;
    private Map<String, Integer> stateIDs;
    private Map<Integer, State> nodes = new HashMap<>();
    private Map<Integer, Set<Integer>> edges = new HashMap<>();
    private Map<Pair<Integer, Integer>, Integer> firedTransitions = new HashMap<>();  /* [src,dst] ~> arc data  */

    StateSpace(int numPlaces) {
        numState = 0;
        stateIDs = new HashMap<>();
        P = numPlaces;
    }

    int addState(Map<Integer, Multiset<Token>> s) {
        numState += 1;
        State state = new State(s);
        System.out.println(String.format("New state: %s ~> %s", numState, state.toString()));
        nodes.put(numState, state);
        stateIDs.put(state.toString(), numState);
        return numState;
    }

    Integer getState(Map<Integer, Multiset<Token>> s) {

        State state = new State(s);
        return stateIDs.get(state.toString());
    }

    void addEdge(int parentID, int childID, int firedTranID) {
        if (edges.containsKey(parentID)) {
            edges.get(parentID).add(childID);
        } else {
            edges.put(parentID, new HashSet<>(childID));
        }
        firedTransitions.put(new Pair<>(parentID, childID), firedTranID);
    }

    Map<Integer, State> getNodes() {
        return nodes;
    }

    Map<Integer, Set<Integer>> getEdges() {
        return edges;
    }

    List<List<Integer>> allPathsBetween(int start, int end, List<Integer> inPath) {

        List<Integer> path = new ArrayList<>();
        for (int i : inPath) {
            path.add(i);
        }
        path.add(start);

        if (start == end) {
            List<List<Integer>> temp = new ArrayList<>();
            temp.add(path);
            return temp;
        }

        if (!edges.containsKey(start)) {
            List<List<Integer>> temp = new ArrayList<>();
            return temp;
        }

        List<List<Integer>> result = new ArrayList<>();

        for (int n : edges.get(start)) {
            if (!path.contains(n)) {
                List<List<Integer>> newPaths = allPathsBetween(n, end, path);
                for (List<Integer> p : newPaths) {
                    result.add(p);
                }
            }
        }
        return result;
    }

    private static String getType(String ty) {
        String[] s = ty.split("_");
        return s[0];
    }

    private static Object getValue(String input, String type) {
        type = getType(type);
        if (type.equals("int")) {
            return Integer.parseInt(input);
        }
        if (type.equals("string")) {
            return input;
        }
        if (type.equals("double")) {
            return Double.parseDouble(input);
        }
        if (type.equals("bool")) {
            return Boolean.parseBoolean(input);
        }
        //type is unit
        return null;
    }

    private ParquetWriter<GenericRecord> parquetWriter(String outputFile, Schema schema) {
        ParquetWriter<GenericRecord> writer = null;
        try {
            writer = AvroParquetWriter.
                    <GenericRecord>builder(new Path(outputFile))
                    .withRowGroupSize(ParquetWriter.DEFAULT_BLOCK_SIZE)
                    .withPageSize(ParquetWriter.DEFAULT_PAGE_SIZE)
                    .withSchema(schema)
                    .withConf(new Configuration())
                    .withCompressionCodec(CompressionCodecName.SNAPPY)
                    .withValidation(false)
                    .withDictionaryEncoding(false)
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return writer;
    }

    /* write Arc in parquet format */
    void parquetWriteArc(Schema schema, String outputFile) {
        GenericRecord record = null;
        try {
            record = new GenericData.Record(schema);
            File t = new File(outputFile);
            t.delete();
            ParquetWriter<GenericRecord> writer = parquetWriter(outputFile, schema);
            for (Pair<Integer, Integer> edge : firedTransitions.keySet()) {
                record.put("src", edge.getValue0());
                record.put("dst", edge.getValue1());
                record.put("transition", firedTransitions.get(edge));
                writer.write(record);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /* write Node in parquet format */
    void parquetWriteNode(Schema schema, String outputFile) {
        Schema listTokenSchema[] = new Schema[(int) P]; // schema of listTokenace = list of Token
        Schema tokenSchema[] = new Schema[(int) P]; // schema of token in listTokenace i-th.
        GenericRecord petriNet;
        GenericArray listToken[] = new GenericData.Array[(int) P];
        GenericRecord token[] = new GenericRecord[(int) P];
        List<List<String>> colSet = new ArrayList<>();
        List<String> oneSet;
        try {
            for (int i = 0; i < P; ++i) {
                listTokenSchema[i] = schema.getField("P" + i).schema();
                tokenSchema[i] = listTokenSchema[i].getElementType();
                oneSet = new ArrayList<>();
                for (Schema.Field color : tokenSchema[i].getFields()) {
                    oneSet.add(color.name());
                }
                colSet.add(oneSet);
            }
            petriNet = new GenericData.Record(schema);
            //System.out.println(schema);
            File t = new File(outputFile);
            t.delete();

            ParquetWriter<GenericRecord> writer = parquetWriter(outputFile, schema);

            String content = new String();
            for (int key : nodes.keySet()) {
                for (int k = 0; k < P; ++k) {
                    int m = nodes.get(key).get(k).size();
                    if (getType(colSet.get(k).get(0)).equals("unit")) {
                        listToken[k] = new GenericData.Array(1, listTokenSchema[k]);
                        token[k] = new GenericData.Record(tokenSchema[k]);
                        token[k].put("unit_0", m);
                        listToken[k].add(0, token[k]);
                    } else {
                        content = Arrays.toString(nodes.get(key).get(k).toArray());
                        if (content.equals("[[]]"))
                            continue;
                        JSONObject object = new JSONObject("{\"listToken\":" + content + "}");
                        JSONArray array = object.getJSONArray("listToken");
                        int h = 0;
                        listToken[k] = new GenericData.Array(m, listTokenSchema[k]);
                        for (Object element : array) {

                            if (element.equals("[[]]"))
                                continue;
                            JSONObject subObj = new JSONObject("{\"Token\":" + element + "}");
                            JSONArray subArr = subObj.getJSONArray("Token");
                            token[k] = new GenericData.Record(tokenSchema[k]);

                            int n = colSet.get(k).size();
                            for (int i = 0; i < n; ++i) {
                                Object value = getValue(subArr.get(i).toString(), colSet.get(k).get(i));
                                token[k].put(colSet.get(k).get(i), value);
                            }
                            listToken[k].add(h, token[k]);
                            h++;
                        }
                    }
                    petriNet.put("id", key);
                    petriNet.put("P" + k, listToken[k]);
                }
                //  System.out.println(petriNet);
                writer.write(petriNet);
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
