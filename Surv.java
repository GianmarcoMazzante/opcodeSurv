import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.script.ScriptException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Surv 
{
	public static void main(String[] args) throws IOException, ParseException, NoSuchMethodException, ScriptException
	{
		Surv s = new Surv();
		
		if(args.length >= 1) // path to file set
		{		
			s.retrieveContractAddresses(args[0], 0, 10);
			s.retrieveContracts(args[0]);
		}
	}
	
		
	public void retrieveContracts(String path) throws IOException, ParseException
	{
		System.out.println("Counting contracts instructions:");
		String ICNVpath = path + "/InstructionCount.json";
		String ICVpath = path + "/InstructionCountVerified.json";
		String ICpath = "";
		String CApath = path + "/contractAddrs.json";
		
		boolean verifiedFlag = false;
		String version = "";
		
		SurvUtils.checkFileExistence(ICNVpath, "{\n}");
		SurvUtils.checkFileExistence(ICVpath, "{\n}");
		SurvUtils.checkFileExistence(CApath, "{\n}");
		
    	JSONParser pparser = new JSONParser();
    	Object pobject = pparser.parse(new FileReader(CApath));
    	JSONObject pjsonObject = (JSONObject) pobject;
    	
    	int lines = pjsonObject.size();
    	int lineCount = 0;
    	int progress = 0, prevProgress=0;
		String outputStr = "";
		String resultStr = "";
		// read the whole file containing the addresses and put it into a buffer
        BufferedReader file = new BufferedReader(new FileReader(CApath));
        String line;
        while ((line = file.readLine()) != null) 
        {
        	if(line.length() > 2) // not '{' nor '}'
            {
        		lineCount++;
    			prevProgress = progress;
    			//lines:100=linec:x
    			progress = lineCount*100/lines;
    			if(progress > prevProgress)
    				System.out.println(progress+"%");
    			
            	String addr = line.substring(2, 44);
            	
            	version = SurvUtils.getContractCompiler(addr);
            	if(version.length() > 0)
            	{
            		ICpath = ICVpath;
            		verifiedFlag = true;
            	}
            	else
            	{
            		ICpath = ICNVpath;
            		verifiedFlag = false;
            	}
            		
            	
            	// open the instruction counter file to check if the current contract was already computed
            	JSONParser aparser = new JSONParser();
            	Object aobject = aparser.parse(new FileReader(ICpath));
            	JSONObject ajsonObject = (JSONObject) aobject;
            	JSONArray check = (JSONArray) ajsonObject.get(addr);
            	if(check != null)
            	{
			    	if(check.size() > 0)
			    		continue;
            	}

            	System.setProperty("http.agent", "Chrome");
        		URL url = new URL("https://etherscan.io/api?module=opcode&action=getopcode&address=" + addr);
                
        		BufferedReader reader = null;
        		try 
        		{
        		   	reader = new BufferedReader(new InputStreamReader(url.openStream()));
        		    String HTMLline = null;
        		    resultStr = "";
        		    while ((HTMLline = reader.readLine()) != null) 
        		    	resultStr += HTMLline;
            		
        		    // the result is in JSON format
        		    JSONParser parser = new JSONParser();
    		        Object object = parser.parse(resultStr);
    		        JSONObject jsonObject = (JSONObject)object;
    		        resultStr = jsonObject.get("result").toString().replaceAll("<br>", "\n");
        		      
    		        Map<String, Integer> instrCount = countInstructions(resultStr);
    		              		        
    		        outputStr = "	\"" + addr + "\":[";
    		        
    		        if(verifiedFlag)
    		        	outputStr = outputStr + "{\"version\":\"" + version + "\"},{\"opcodes\":[";   
    		           		        
    		        for (Map.Entry<String, Integer> entry : instrCount.entrySet()) 
    		        {
    		        	outputStr = outputStr + "{\"" + entry.getKey() + "\":" + entry.getValue() + "}";
    		        }
    		        if(verifiedFlag)
    		        	outputStr = outputStr + "]}";   
    		        outputStr = outputStr  + "],\n";
    		        	  
    			    // add addr on the JSON list
    		        SurvUtils.removeClosingBracket(ICpath);
    		        try (FileWriter fileOut = new FileWriter(ICpath, true)) 
    				{	
    					fileOut.append(outputStr);
    				}    		            		        
			        SurvUtils.addClosingBracket(ICpath);

        		}
        	    finally 
        	    {
        	    	
        	    	if (reader != null) reader.close();
        	    }	
        			
            }
        	
        }
        file.close();
 
          
	}
	
	/**
	 * Save on a JSON file all the addresses of every contracts and the block it belongs to. 
	 * It takes a block range for easy updates of the file 
	 * @param path
	 * @param startingBlock
	 * @param limitBlock
	 * @throws IOException
	 * @throws ParseException
	 */
	@SuppressWarnings("unchecked")
	public void retrieveContractAddresses(String path, int startingBlock, int limitBlock) throws IOException, ParseException
	{		
		int progress = 0, prevProgress = 0;
		int cntrs = 0; // count of addresses
		int block=startingBlock;
		int txsPage=1;
		boolean breakFlag = false;
		
		System.out.println("Retrieving contract addresses:");
		path += "/contractAddrs.json";
		SurvUtils.checkFileExistence(path, "{\n}");
		
		JSONObject cntrt = new JSONObject();
		SurvUtils.removeClosingBracket(path); // get the len after removing the bracket
        
		System.setProperty("http.agent", "Chrome");
		
		while(block<=limitBlock)
		{	
			prevProgress = progress;
			progress = (block-startingBlock)*100/(limitBlock-startingBlock);
			if(progress > prevProgress)
				System.out.println(progress+"%");
			
			URL url = new URL("https://etherscan.io/txs?block="+block+"&ps=100&p="+txsPage);
			
			BufferedReader reader = null;
			try 
			{
				reader = new BufferedReader(new InputStreamReader(url.openStream()));
		        String line = null;
		        while ((line = reader.readLine()) != null) 
		        {
		        	if(line.toLowerCase().contains("<tr><td>"))
		        	{
			        	if(line.toLowerCase().contains("there are no matching entries"))
			        	{
			        		breakFlag = true;
			        		break;
			        	}
			        	else if(line.toLowerCase().contains("contract creation")) // take only the contracts when they are created	
			        	{
			        		String[] TDtokens = line.split("<tr>"); // the table is actually written on 1 line, so it needs to be splitted in table rows
				        	for (String token : TDtokens)
			        		{ 
				        		if(token.toLowerCase().contains("contract creation") && !token.toLowerCase().contains("fa fa-exclamation-circle")) // the row must contain the contract creation and the transaction must be successful	
					        	{
				        			int cc = token.toLowerCase().indexOf("contract creation");
				        			String addr = token.substring(cc-44,cc-2);
				        			
				        			String date = token.substring(token.indexOf("/block/" + block) + 73 + (block+"").length()*2 );
				        			date = date.substring(0, date.indexOf(" "));
				        	
				        			String blockNdate = "[{\"block\":" + block + "},{\"date\":\"" + date + "\"}]";
				        			cntrt.clear();
			        		        cntrt.put(addr, blockNdate);
			        		        
				        			// add addr on the JSON list
			        		        try (FileWriter file = new FileWriter(path, true)) 
			        				{	
			        					file.append("	\"" + addr + "\":" + blockNdate + ",\n");
			        					cntrs++;
			        				}
				        			
					        	}
			        		}
				        	break;
			        	}
			        	else
			        	{
			        		break;
			        	}
		        	}
		        }	
		        txsPage++;
		        if(breakFlag)
		        {
	        		txsPage=1;
	        		block++;
	        		breakFlag=false;
		        }
			}
			finally 
		    {
		    	if (reader != null) reader.close();
		    }	
		}
		SurvUtils.addClosingBracket(path);
	    System.out.println(cntrs + " contracts added");
	}
	
	/**
	 * It creates a map with all the instructions and number of occurrences of them.
	 * 
	 * @param opcodeContract
	 * @return
	 */
	private Map<String, Integer> countInstructions(String opcodeContract)
	{
		Map<String, Integer> instrCount = new HashMap<String, Integer>();
		StringTokenizer st2 = new StringTokenizer(opcodeContract, "\n");
        while (st2.hasMoreTokens()) 
        {
        	String nextToken = st2.nextToken();
      	
        	if(nextToken.contains(" "))
        		nextToken = nextToken.substring(0, nextToken.indexOf(" "));
        
        	if(nextToken.toLowerCase().contains("unknown"))
        		nextToken = SurvUtils.translateUnknownBytecode(nextToken);
        		
      		instrCount.put(nextToken, instrCount.get(nextToken)==null ? 1: instrCount.get(nextToken)+1);
        }
        return instrCount;
	}
	


}

