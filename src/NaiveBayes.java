import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

class Pair {
  private int x;
  private int y;
  
  Pair(int x, int y){
    this.x = x;
    this.y = y;
  }
  public int getX() {
    return x;
  }
  public void setX(int x) {
    this.x = x;
  }
  public int getY() {
    return y;
  }
  public void setY(int y) {
    this.y = y;
  }
}

class tuple {
  private Pair coordinate;
  private char value;
  public Pair getCoordinate() {
    return coordinate;
  }
  public void setCoordinate(Pair coordinate) {
    this.coordinate = coordinate;
  }
  public char getValue() {
    return value;
  }
  public void setValue(char value) {
    this.value = value;
  }
  
}

public class NaiveBayes {

  static HashMap<Integer, ArrayList<HashMap<Pair, Character>>> imageDatabase; 
  static HashMap<Integer, HashMap<Pair, Double>> condProbBackground;
  //static HashMap<Integer, HashMap<Pair, Double>> condProbForeground;
  static int maxRows = 100;
  
  static int numCols = 28;
  static int numRows = 28;
  
  static Pair pairMapper[][];
  static double P[];
  
  public static void testRead(){
    for(int i = 0 ; i < 10 ; i ++){
      System.out.println("Images for " + i);
      System.out.println();
      
      ArrayList<HashMap<Pair, Character>> tempArr;
      tempArr = imageDatabase.get(i);
      HashMap<Pair, Character> map;
      
      for(int k = 0 ; k < tempArr.size(); k ++){
        map = tempArr.get(k);
        printHashMap(map);
      }
    }
  }
  
  public static void printHashMap(HashMap<Pair, Character> map){
    //System.out.println(map.keySet().size());
    for(int m = 0 ; m < numRows; m ++){
      for(int n = 0 ; n < numCols; n++){
          System.out.print(map.get(pairMapper[m][n]));
      }
      System.out.println();
    }
    
    System.out.println();
    
  }
  
  public static void calculateConditionalProbabilities() throws Exception{
    for(int digit = 0 ; digit < 10 ; digit ++){
      //get all the digits in digitData
      ArrayList<HashMap<Pair, Character>> digitData = imageDatabase.get(digit);
      HashMap<Pair, Double> probMap = new HashMap<Pair, Double>();

      for(int i = 0 ; i < numRows; i ++) {
        for(int j = 0 ; j < numCols; j++) {
          int spaces = 0;
          int plus = 0;
          int hash = 0;
          
          for(int image  = 0 ; image < digitData.size() ; image++) {
              if(digitData.get(image).containsKey(pairMapper[i][j])){
                char ch = digitData.get(image).get(pairMapper[i][j]);
                if(ch == ' '){
                  spaces ++;
                }
                else if(ch == '+'){
                  plus ++;
                }
                else if(ch == '#'){
                  hash ++;
                }
                else{
                  throw new Exception("Unexpected Character");
                }
              }
          }
          double backProb;
          //ensure non zero probabilities
          
          if(spaces == 0){
            backProb = 1/(spaces + plus + hash + 2);
          }
          else if ((plus + hash) == 0){
            backProb = 1.0 - 1/(spaces + plus + hash + 2);
          }
          else{
            backProb = (spaces*1.0) / (spaces + plus + hash);
          }
          probMap.put(pairMapper[i][j], backProb);
        }
      }
      condProbBackground.put(digit, probMap);
      //System.out.println("\nMaxRows: "+ maxNumRows);

    }
  }

  /**
   * @param args
   * @throws Exception 
   */
  @SuppressWarnings("deprecation")
  public static void main(String[] args) throws Exception {
    condProbBackground = new HashMap<Integer, HashMap<Pair, Double>>();
    imageDatabase = new HashMap<Integer, ArrayList<HashMap<Pair, Character>>> ();
    
    if(args.length < 5){
      System.out.println("Usage: java NaiveBayes <path/to/trainingimages> <path/to/traininglabels>" +
      		"<path/to/testimages> <path/to/testlabels> <path/to/classifiedOutput>");
      return;
    }
    
    String trainingDataPath = args[0];
    String trainingLabelsPath = args[1];
    String line;
    int i = 0 ;
    int j = 0;
    int label = 0;
    int numRecords = 0;
    P = new double[10];
    
    //keeps count from 0-9
    int count[] = new int[10];
    pairMapper= new Pair[numRows][numCols];
    for(i = 0 ; i < numRows; i ++){
      for(j = 0 ; j < numCols; j ++){
        pairMapper[i][j] = new Pair(i,j);
      }
    }
    
    ArrayList<HashMap<Pair, Character>> tempArrayList;
    FileInputStream fstream = new FileInputStream(trainingDataPath);
    DataInputStream in = new DataInputStream(fstream);

    FileInputStream labelStream = new FileInputStream(trainingLabelsPath);
    DataInputStream labelIn = new DataInputStream(labelStream);

    // read blank lines
    line = in.readLine();
    while(line!=null) {
      HashMap<Pair, Character> currentHashMap = new HashMap<Pair, Character>();

      //image data copying started
      for(i = 0; i < numRows; i ++){
        for(j = 0 ; j < numCols; j ++) {
          char ch = line.charAt(j);
          currentHashMap.put(pairMapper[i][j],ch);
        }
        line = in.readLine();
      }
      
      //image copying data stopped
      label = Integer.parseInt(labelIn.readLine());
      count[label]++;
      numRecords++;
      //pick up existing arraylist
      if(imageDatabase.containsKey(label)){
        tempArrayList = imageDatabase.get(label);
      }
      // allocate a new tempArrayList
      else{
        tempArrayList = new ArrayList<HashMap<Pair, Character>>();
      }
      tempArrayList.add(currentHashMap);
      imageDatabase.put(label, tempArrayList);
      //Look for new lines
    }
    
    in.close();
    labelIn.close();
    fstream.close();
    labelStream.close();
    
    //Calculate Probabilities
    for(i = 0 ; i < 10 ; i ++){
      P[i] = (count[i]*1.0)/numRecords;
    } 
    //Trains the dataset
    calculateConditionalProbabilities();
    classify(args[2], args[3], args[4]);
    
  }
  
  @SuppressWarnings("deprecation")
  public static void classify(String inputPath, String actualOutput, String outputPath) throws Exception{
    int correctLabels = 0;
    int totalData = 0;
    FileInputStream fstream = new FileInputStream(inputPath);
    DataInputStream in = new DataInputStream(fstream);
    int i,j;
    String line = in.readLine();
    HashMap<Pair, Character> currentHashMap = new HashMap<Pair, Character>();
    //true positive
    int tp[] = new int[10];
    //false positive
    int fp[] = new int[10];
    //false negative
    int fn[] = new int[10];
    
    FileInputStream labelStream = new FileInputStream(actualOutput);
    DataInputStream labelIn = new DataInputStream(labelStream);
    PrintWriter out  = new PrintWriter(new BufferedWriter(new FileWriter(outputPath)));
    
    while(line!=null) {
      //A test image copied into currentHashMap
      for(i = 0; i < numRows; i ++){
        for(j = 0 ; j < numCols; j ++) {
          char ch = line.charAt(j);
          currentHashMap.put(pairMapper[i][j],ch);
        }
        line = in.readLine();
      }
      totalData ++;
      //CurrentHashMap contains current image
      int label = getClassifiedLabel(currentHashMap);
      int actualLabel = Integer.parseInt(labelIn.readLine());
      
      if(label == actualLabel){
        correctLabels ++;
        tp[label] ++;
      }
      else{
        fp[label] ++;
        fn[actualLabel] ++;
      }
      out.write(label+"\n");
      out.flush();
      currentHashMap.clear();
    }
    out.close();
    double accuracy  = correctLabels*100.0 / totalData;
    System.out.println("Accuracy = " + accuracy + "%");
    for(i = 0 ; i < 10; i ++){
      double prec = tp[i]*1.0/(tp[i] + fp[i]);
      System.out.print("Precision of "+ i +" = " + prec );
      double recall = tp[i]*1.0 / (tp[i] + fn[i]);
      System.out.println(" Recall of "+ i +" = " + recall );
      
    }
  }
  
  public static int getClassifiedLabel(HashMap<Pair, Character> currentHashMap){
    
    double maxProb = 0;
    int maxDigit = -1;
    
    for(int digit = 0 ; digit < 10 ; digit ++){
      
      double LogProbDigit = Math.log(P[digit]);
      
      for(int i = 0 ; i < numRows; i ++){
        for(int j = 0 ; j < numCols; j ++){
      
          char ch = currentHashMap.get(pairMapper[i][j]);
          //Background character
          if(ch == ' ') {
            LogProbDigit = LogProbDigit  + Math.log(condProbBackground.get(digit).get(pairMapper[i][j]));
          }
          //foreground character
          else{
            LogProbDigit = LogProbDigit + Math.log(1 - condProbBackground.get(digit).get(pairMapper[i][j]) ) ;
          }
        }
      }
      
      if(digit ==0){
        maxProb = LogProbDigit;
        maxDigit = 0;
      }
      
      else if(LogProbDigit > maxProb){
        maxProb = LogProbDigit;
        maxDigit = digit;
      }
      
    }
    
    //We did find a maxDigit
    assert(maxDigit!=-1);
    
    return maxDigit;
  }


}
