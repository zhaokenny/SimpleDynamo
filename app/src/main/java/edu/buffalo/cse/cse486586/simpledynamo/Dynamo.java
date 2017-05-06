package edu.buffalo.cse.cse486586.simpledynamo;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.concurrent.ConcurrentHashMap;

public class Dynamo {
    private static final String DYNAMO_TAG = "Dynamo";
    public static final String[] nodes = {"5562", "5556", "5554", "5558", "5560"};
    public static final int[] ports = {11124, 11112, 11108, 11116, 11120};
    private static final int PREF_SIZE = 3;

    private String[] hashedNodes;
    private ConcurrentHashMap<String, String> nodeIdTranslator;

    public Dynamo(){
        hashedNodes = generateHashedNodes();
        nodeIdTranslator = new ConcurrentHashMap<String, String>();
        populateTranslator(nodeIdTranslator);
    }

    private String[] generateHashedNodes(){
        String[] hashedArr = new String[nodes.length];
        try{
            for(int i = 0; i < hashedArr.length; i++){
                hashedArr[i] = genHash(nodes[i]);
            }
        } catch(NoSuchAlgorithmException e){
            e.printStackTrace();
            Log.e("Dynamo", "NoSuchAlgorithmException in generateHashedNodes from Dynamo.");
        }
        return hashedArr;
    }

    private void populateTranslator(ConcurrentHashMap<String,String> translator){
        for(String node : nodes){
            try {
                translator.put(node, genHash(node));
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                Log.e(DYNAMO_TAG, " Failed to add node to nodeIdTranslator");
            }
        }
    }
    public String[] getHashedNodes(){
        return hashedNodes;
    }

    // Generates an array of ports that the given key needs to be replicated to.
    public int[] getPrefList(String key){
        try {
            String hashedKey = genHash(key);
            int[] prefList = new int[PREF_SIZE];

            int coordinatorIndex = getCoordinatorIndex(hashedKey);
            Log.e(DYNAMO_TAG, "(getPrefList) Hash value for key " + key + " is " + hashedKey + " and its coordinator is " + ports[coordinatorIndex]);
            for(int i = 0; i < prefList.length; i++){
                prefList[i] = ports[coordinatorIndex];
                Log.e(DYNAMO_TAG, "PrefList index " + i + " has port " + prefList[i] + " and corresponds with hashNode " + hashedNodes[coordinatorIndex]);
                coordinatorIndex = (coordinatorIndex + 1) % ports.length;
            }
            return prefList;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Log.e(DYNAMO_TAG, "NoSuchAlgorithmException in getPrefList from Dynamo.");
            return null;
        }
    }

    private int getCoordinatorIndex(String hashedKey){
        for(int i = 1; i < hashedNodes.length; i++){
            if(hashedKey.compareTo(hashedNodes[i]) <= 0 && hashedKey.compareTo(hashedNodes[i-1]) > 0 ){
                return i;
            }
        }
        return 0;
    }

    public boolean isInCorrectPartition(String key, int port){
        int[] prefList = getPrefList(key);

        for(int replica : prefList){
            if(port == replica){
                Log.e(DYNAMO_TAG, " Key " + key + " belongs in port " + port);
                return true;
            }
        }
        return false;
    }

    public String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
