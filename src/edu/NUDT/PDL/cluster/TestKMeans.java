package edu.NUDT.PDL.cluster;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import edu.NUDT.PDL.util.Reporters;



public class TestKMeans {

    static Set<double[]> randomDenseElts(int numElts, int numDims, Random random) {
        Set<double[]> inputSet = new HashSet<double[]>(numElts*2);
        for (int i = 0; i < numElts; ++i)
            inputSet.add(randomDenseElt(numDims,random));
        return inputSet;
    }

    static double[] randomDenseElt(int numDims, Random random) {
        double[] xs = new double[numDims];
        for (int i = 0; i < xs.length; ++i)
            xs[i] =  10.0 * random.nextDouble() - 5.0;
        return xs;
    }

    static FeatureExtractor<double[]> ID_PARSER
        = new FeatureExtractor<double[]>() {
        public Map<String,? extends Number> features(double[] xs) {
            Map<String,Double> result = new HashMap<String,Double>();
            for (int i = 0; i < xs.length; ++i)
                result.put(Integer.toString(i), Double.valueOf(xs[i]));
            return result;
        }
    };

    // @Test // need to scale down for release and remove main!
    public void testKMeansPlusPlus() {
        int K = 10; // 1000; // 
        int n = 8903; // 167194; // 
        int d = 50;

        int maxEpochs = 200;
        double minImprovement = 0.0001;
        KMeansClusterer clustererKmpp
            = new KMeansClusterer(ID_PARSER,K,maxEpochs,true,minImprovement);

        KMeansClusterer clusterer
            = new KMeansClusterer(ID_PARSER,K,maxEpochs,false,minImprovement);

        Random random = new Random(); // (747L);

        Set<double[]> eltSet = randomDenseElts(n,d,random);

        Set<Set<double[]>> clustering
            = clusterer.cluster(eltSet,random,Reporters.stdOut());
        print(clustering);
    }


    private void print(Set<Set<double[]>> clustering) {
		// TODO Auto-generated method stub
		Iterator<Set<double[]>> ier = clustering.iterator();
		int index=0;
		int total=0;
		while(ier.hasNext()){
			Set<double[]> tmp = ier.next();
			System.out.println(index+" th cluster: "+tmp.size());
			total+=tmp.size();
			index++;
		}
		System.out.println("Total: "+total);
	}

	public static void main(String[] args) {
        new TestKMeans().testKMeansPlusPlus();
    }
    
}
