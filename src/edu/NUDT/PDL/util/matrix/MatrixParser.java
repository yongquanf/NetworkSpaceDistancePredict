package edu.NUDT.PDL.util.matrix;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Vector;

import edu.NUDT.PDL.RatingFunc.MathUtil;

public class MatrixParser {

	public class record{
		public String sp;	//start
		public String ep;	//end
		public float latency;//latency
		}	
		public Vector<HashMap<Integer, record>> latencyList; // load the latency matrix into a vector of vector for each point, sorted by rows 
		public record cuRecord;
		public static Vector<record> recordList;
		public double[][] latencyMatrix;
		public double[][] CoordMatrix;
		public Vector<InetAddress> nodes;
		public int rows;// -1
		public int colums;
		public int format;	
		public int line;
		public int[] badNodes;	//'bad' landmarks
		public int starts0;
		/*
		 * format:
		 * 1:  a b a_bLatency	default
		 * 2: row, colum, latency matrix
		 * 3: row ,colum, address  latency
		 * ...
		 * 
		*/
		public MatrixParser(){
			latencyList=new Vector<HashMap<Integer, record>>();
			cuRecord=null;
			recordList=new Vector<record>(200,100);
			latencyMatrix=null;
			CoordMatrix=null;
			rows=colums=Integer.MIN_VALUE;
			format=1;	
			line=-1;
			nodes=new Vector<InetAddress>(200,50);
			starts0=0;
			
		}
		/*
		 * read a truelatency matrix,
		 * format:
		 * a b lantency
		 */
		public boolean readlatencyMatrix(File truelatency){
			int temp0,temp1;
		    line=-1;
		    double val;
			if(truelatency!=null){
				try{
					RandomAccessFile rf=new RandomAccessFile(truelatency, "r");  
					if(rf==null){
						System.err.println("ERROR, empty file  !!");
						return false;
					}
					else{
						String cuLine;
						
						while(true){
						
							cuLine=rf.readLine();
							line++;			//current line
							//System.out.println("@: CurLine "+cuLine);
							if(cuLine==null){
								System.out.println("@: Panic "+line);
								break;
							}
							if(cuLine.startsWith("#")){
								continue;
							}
							//write cuLine into record list
							//System.out.println("@: CurLine "+cuLine);
							String []s=cuLine.split("[ \\s\t ]");	//seperated by " "//XMatrix
							//System.out.format("%s",cuLine);
							//System.out.println();
							//===================================================
							if(getFormat()==1){
								if(line==0){
									/*row*/
									rows=Integer.parseInt(s[0]);
								}
								else if(line==1){
									colums=Integer.parseInt(s[0]);
									System.out.format("$ array is%d %d\n",rows,colums);
									initMatrix();
									
								}
								else {
									//System.out.println("% "+s.length+" ");
									
									//======================================
									int a=Integer.parseInt(s[0]);
									int b=Integer.parseInt(s[1]);
									
									//TODO:start from 1
									double v=Double.parseDouble(s[2]);
									
									
									latencyMatrix[a-this.starts0][b-this.starts0]=v;
									latencyMatrix[b-this.starts0][a-this.starts0]=v;
									//System.out.println("% "+v+ " "+latencyMatrix[a-this.starts0][b-this.starts0]+" ,"+latencyMatrix[b-this.starts0][a-this.starts0]);
									//badNodes[a]--;
							
								}
							
							//System.out.format("$ is%d %d %d %d \n",temp0,temp1,rows,colums);
						
							}
							else if(format==2){
								//TODO: error when matlab save data 
								/* latency matrix */
								if(line==0){
									/*row*/
									this.rows=Integer.parseInt(s[0]);
									System.out.println("$ rows: "+this.rows);
								}
								else if(line==1){
									this.colums=Integer.parseInt(s[0]);
									System.out.format("$ array is%d %d\n",rows,colums);
									initMatrix();
									//MinMatrix();
									//initbadNodes(colums);
								}
								else{
									//System.out.println("% "+s.length+" ");
									int start=0;
									int end=s.length;
									//TODO seperate by pattern
									//System.out.println("% start"+start+"  end "+end);
								    if(s[start].isEmpty()){
								    	start++;
								    }
									for(int i=0;start<end;){
									//latencyMatrix[line-2][i]=Float.parseFloat(s[i]);
										if(s[start].isEmpty()){
											//nothing
										}
										else{
											val=Double.parseDouble(s[start].trim());
											if(val<0){
												val=0;
											}
									latencyMatrix[line-2][i]=val;
										i++;
										}
									//System.out.format(" <"+(line-2)+","+i+"> %f ",latencyMatrix[line-2][i]);
									start++;
									}
									//diagonal element
									latencyMatrix[line-2][line-2]=0;
									//System.out.println();
								}
								}
							
							else if(getFormat()==3){
								/*address matrix */
								if(line==0){
									/*row*/
									this.rows=Integer.parseInt(s[0]);
								}
								else if(line==1){
									this.colums=Integer.parseInt(s[0]);
									initMatrix();
								}
								else{
									nodes.add(InetAddress.getByName(s[0]));
									for(int i=0;i<colums;i++){
									latencyMatrix[line][i]=Float.parseFloat(s[i+1]);
									}
								
								}
							}
												
						}
						
					}
					System.out.println("@: Panic "+line);
					rf.close();
					return true;
				
				}catch(Exception e)
				{		
				}
			}
			System.err.println(" empty file");
			
			return false;
		}
		/**
		 * with -1
		 *
		 */
		public void initMatrix(){
			latencyMatrix =new double[rows][colums];
			for(int i=0;i<rows;i++){
				for(int j=0;j<rows;j++)
					latencyMatrix[i][j]=-1;
			}
			
		}
		public boolean writeLatencyMatrix(File truelatency){
			this.readlatencyMatrix(truelatency);
			try {
				BufferedWriter b=getWriter(new File(truelatency+".out"));
				int count =0;
				for(int i=0;i<rows;i++){
					for(int j=0;j<colums;j++){
						if(i!=j&&latencyMatrix[i][j]==-1){
							System.out.println(" "+i+" "+j);
							count++;
							latencyMatrix[i][j]=0;
						}
						else if(i==j){
							latencyMatrix[i][j]=0;
						}
						b.write(latencyMatrix[i][j]+" ");
					}
					
					b.newLine();
				}
				System.out.println("\n================\n"+count);
			b.flush();
			b.close();
			return true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
			
		}
	 public  BufferedWriter getWriter(File aFile)
	      throws IOException
	  {
	      return new BufferedWriter(new FileWriter(aFile));
	  }
		public boolean ReadCoordinate(File truelatency){
			int temp0,temp1;
		    line=-1;
			if(truelatency!=null){
				try{
					RandomAccessFile rf=new RandomAccessFile(truelatency, "r");  
					if(rf==null){
						return false;
					}
					else{
						String cuLine;
						
						while(true){
						
							cuLine=rf.readLine();
							line++;			//current line
							System.out.println("@: CurLine "+cuLine);
							if(cuLine==null){
								System.out.println("@: Panic "+line);
								break;
							}
							if(cuLine.startsWith("#")){
								continue;
							}
							//write cuLine into record list
							//System.out.println("@: CurLine "+cuLine);
							if(line==0){
								/*row*/
								rows=Integer.parseInt(cuLine.trim());
							}
							else if(line==1){
								colums=Integer.parseInt(cuLine.trim());
								System.out.format("$ array is%d %d\n",rows,colums);
								CoordMatrix =new double[rows][colums];
								//MinMatrix();
								//initbadNodes(colums);
							}
							else{
								String []s=cuLine.split
								("[   \t]");	//seperated by " "//XMatrix
								//coordinate
								System.out.println("% "+s.length+" ");
								int start=1;
								int end=s.length-1;
								//TODO seperate by pattern
								System.out.println("% start"+start+"  end "+end);
								for(int i=start;i<end+1;i++){
									System.out.println("% "+i+"  : "+s[i]);
								}
								for(int i=0;i<this.colums;i++){
								//latencyMatrix[line-2][i]=Float.parseFloat(s[i]);
									while(s[i+start].trim().isEmpty()||s[i+start].trim()==null){
										start++;
									}
									CoordMatrix[line-2][i]=Float.parseFloat(s[i+start].trim());
								System.out.format(" %f ",CoordMatrix[line-2][i]);
								}
							}
						
						}
					}
				}catch(Exception e){
					System.out.println(e.toString());
				};
		
						}
			return true;
		}
		/**
		 * write to this.latencyMatrix
		 * @return
		 */
		public boolean writePairwiseDistance(){
			if(this.CoordMatrix!=null){
				int nodes=this.rows;
				int dim=this.colums;
				MathUtil math =new MathUtil(100);
				double dis;
				if(this.latencyMatrix==null){
					this.latencyMatrix=new double[nodes][nodes];
				}
				else{
					//nana
				}
				 try {
					for(int index=0;index<nodes;index++){
						for(int to=index;to<nodes;to++){
							dis=math.linear_distance(this.CoordMatrix[index], 0, this.CoordMatrix[to], 0, dim, 2);
							this.latencyMatrix[index][to]=this.latencyMatrix[to][index]=dis;
						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return true;
			}
			return false;
		}
		/**
		 * write pairwise distance matrix
		 * i,j dis
		 * @return
		 */
		public boolean writePairwiseDistance(String fileName){
			if(this.CoordMatrix!=null){
				int nodes=this.rows;
				int dim=this.colums;
				MathUtil math =new MathUtil(100);
				double dis;
				 try {
					PrintStream ps=new PrintStream(new File(fileName));
					for(int index=0;index<nodes;index++){
						for(int to=0;to<nodes;to++){
							dis=math.linear_distance(this.CoordMatrix[index], 0, this.CoordMatrix[to], 0, dim, 2);
							ps.println(index+" "+to+" "+dis);
						}
					}
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return true;
			}
			return false;
		}
		public void MinMatrix(){
			if(latencyMatrix!=null){
				for(int i=rows;i>0;i--){
					for(int j=colums;j>0;j--){
						latencyMatrix[i-1][j-1]=Float.MIN_VALUE;
					}
				}
			}
		}
		/*
		 * read a icmprecords, latency measurements may miss
		 */
		public boolean readlatencyMeasurements(File icmprecords){
		
			return true;
		}
		public void initbadNodes(int colums){
			badNodes=new int[colums];
			for(int i=0;i<colums;i++){
				badNodes[i]=colums;
			}
		}
		/**
		 * list the 'correct' landmark candidates
		 * @return 'correct' landmarks
		 */
		public int[] landmarkCandidates(){
			return null;
		}
		/*
		 * if the recordlist not empty, fill the matrix
		 * we have to readlatencyMatrix first.
		 * format:
		 * a b latency
		 */
		public boolean fillLatencyMatrix(){
			int recordSize=recordList.size();
			if(recordSize>0){
				if(getFormat()==1){
				rows++;
				colums++;
				latencyMatrix=new double[this.rows+1][this.colums+1];
				for(int i=0;i<=this.rows;i++){
					for(int j=0;j<=this.colums;j++){
						latencyMatrix[i][j]=Float.MIN_VALUE;
					}
				}
				System.out.println("\n$ Matrix nodes: rows"+(this.rows+1)+"colums"+(this.colums+1));
				int index=0;
				
				while(recordSize>0){
					record r=recordList.elementAt(index++);
				//	System.out.println("\n$ Matrix nodes:"+r.sp+"\t:"+r.ep);
					latencyMatrix[Integer.parseInt(r.sp)][Integer.parseInt(r.ep)]=r.latency;
					//latencyMatrix[r.ep][r.sp]=r.latency; // assume symmetric duplex
					recordSize--;
				}
				}
				return true;
			}
			
			return false;
			
		}
		public int getRows(){
			//System.out.println("$: ROWS "+this.rows);
			return this.rows;
		}
		public int getColums(){
			return this.colums;
		}
		/*
		 * get the latency in(row,colum) 
		 */
		public float get(int row,int colum){
		
			//System.out.println("Now we have row:"+row+" colum:"+colum);
			return (float)latencyMatrix[row][colum];
			
		
		}
		public boolean exists(int row,int colum){
			if(latencyMatrix[row][colum]!=Float.MIN_VALUE){
				return true;
			}
			return false;
		}
		public int getFormat() {
			return format;
		}
		public void setFormat(int format) {
			this.format = format;
		}
		
		
		/**
		 * return as a matrix
		 * @return
		 */
		public Matrix asMatrix(){
			//
			return new DenseMatrix(this.latencyMatrix);
		}
}