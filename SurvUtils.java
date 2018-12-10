import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.URL;

public class SurvUtils 
{
	/**
	 * Check if the given block exists
	 * @param block
	 * @throws IOException 
	 */
	protected static boolean checkBlockExistence(int block) throws IOException 
	{
		boolean exists = true;
		
		System.setProperty("http.agent", "Chrome");
		URL url = new URL("https://etherscan.io/block/"+block);
		BufferedReader reader = null;
		try 
		{
			reader = new BufferedReader(new InputStreamReader(url.openStream()));
	        String line = null;
	        while ((line = reader.readLine()) != null) 
	        {
	        	if(line.toLowerCase().contains("unable to locate block"))
	        	{
	        		exists = false;
	        		break;
	        	}
	        		
	        }
		}
		finally 
	    {
	    	if (reader != null) reader.close();
	    }	
		
		return exists;
	}
	
	/**
	 * If the file specified on the path doesn't exists then a new file is created
	 * with 2 lines: the first one with the opening bracket and the second one with the
	 * closing bracket, so it does not interfere with the JSON parser.
	 * @param path, init
	 * @throws IOException
	 */
	protected static void checkFileExistence(String path, String init) throws IOException
	{
		File f = new File(path);
		if(!f.exists())
		{
			f.createNewFile();	
			try (FileWriter file = new FileWriter(path)) 
			{	
				file.write(init);
			}
		}
	}
	
	/**
	 * To append lines the last bracket should be removed. To avoid problems with
	 * the new line char the whole line is removed
	 * @param path
	 * @return the length of the file
	 * @throws IOException
	 */
	protected static void removeClosingBracket(String path) throws IOException
	{
	   RandomAccessFile cf = new RandomAccessFile(path, "rw");
	   long clength = cf.length() - 1;
	   byte cb = 0;
	   do 
	   {                     
		   clength -= 1;
		   cf.seek(clength);
		   cb = cf.readByte();
		} while(cb != 10);
		cf.setLength(clength+1);
		cf.close();
		
		if(clength > 1) //there are entries
			SurvUtils.correctComma(path);
	}
	
	/**
	 * at the end the closing bracket should be added (and in some cases the last comma must be removed)
	 * @param path
	 * @throws IOException
	 */
	protected static void addClosingBracket(String path) throws IOException
	{
		RandomAccessFile lf = new RandomAccessFile(path, "rw");
		long length = lf.length() - 1;
		
		if(length > 2)
			lf.setLength(length-1);		
	    lf.close();	
	    		
		// in every cases the closing bracket should be added
		try (FileWriter file = new FileWriter(path, true)) 
		{
			file.append("\n}");
		}
	}
	
	

	/**
	 * On the list, the last addr doesn't have the comma used to separate different objects on JSON.
	 * Before adding the comma the new line char should be removed and then added again after the comma.
	 * @param path
	 * @throws IOException
	 */
	protected static void correctComma(String path) throws IOException
	{
		RandomAccessFile clf = new RandomAccessFile(path, "rw");
		clf.setLength(clf.length() - 1);
		clf.close();
        try (FileWriter file = new FileWriter(path, true)) 
		{	
			file.append(",\n");
		}
	}
	
	protected static String getContractCompiler(String addr) throws IOException
	{
		String compiler = "";
		
		System.setProperty("http.agent", "Chrome");
		URL url = new URL("https://etherscan.io/address/" + addr + "#code");
		BufferedReader reader = null;
		try 
		{
			reader = new BufferedReader(new InputStreamReader(url.openStream()));
		    String line = null;
		    while ((line = reader.readLine()) != null) 
		    {
		    	if(line.toLowerCase().contains("v0."))
		    	{
		    		compiler = line;
		    		if(compiler.contains("+"))
		    			compiler = compiler.substring(0, compiler.indexOf("+"));
		    		break;
		    	}
		    	
		    	if(line.toLowerCase().contains("are you the contract creator"))
		    		break;
		    }
		}
		finally 
	    {
	    	if (reader != null) reader.close();
	    }	
		return compiler;
	}
	
	protected static String translateUnknownBytecode(String bytecode)
	{
		
		bytecode = bytecode.substring(1, 3).toLowerCase();
		String opcode = bytecode;
		
		if(bytecode.equals("00")) opcode="STOP";
		if(bytecode.equals("01")) opcode="ADD";
		if(bytecode.equals("02")) opcode="MUL";
		if(bytecode.equals("03")) opcode="SUB";
		if(bytecode.equals("04")) opcode="DIV";
		if(bytecode.equals("05")) opcode="SDIV";
		if(bytecode.equals("06")) opcode="MOD";
		if(bytecode.equals("07")) opcode="SMOD";
		if(bytecode.equals("08")) opcode="ADDMOD";
		if(bytecode.equals("09")) opcode="MULMOD";
		if(bytecode.equals("0a")) opcode="EXP";
		if(bytecode.equals("0b")) opcode="SIGNEXTEND";
		if(bytecode.equals("10")) opcode="LT";
		if(bytecode.equals("11")) opcode="GT";
		if(bytecode.equals("12")) opcode="SLT";
		if(bytecode.equals("13")) opcode="SGT";
		if(bytecode.equals("14")) opcode="EQ";
		if(bytecode.equals("15")) opcode="ISZERO";
		if(bytecode.equals("16")) opcode="AND";
		if(bytecode.equals("17")) opcode="OR";
		if(bytecode.equals("18")) opcode="XOR";
		if(bytecode.equals("19")) opcode="NOT";
		if(bytecode.equals("1a")) opcode="BYTE";
		if(bytecode.equals("20")) opcode="SHA3";
		if(bytecode.equals("30")) opcode="ADDRESS";
		if(bytecode.equals("31")) opcode="BALANCE";
		if(bytecode.equals("32")) opcode="ORIGIN";
		if(bytecode.equals("33")) opcode="CALLER";
		if(bytecode.equals("34")) opcode="CALLVALUE";
		if(bytecode.equals("35")) opcode="CALLDATALOAD";
		if(bytecode.equals("36")) opcode="CALLDATASIZE";
		if(bytecode.equals("37")) opcode="CALLDATACOPY";
		if(bytecode.equals("38")) opcode="CODESIZE";
		if(bytecode.equals("39")) opcode="CODECOPY";
		if(bytecode.equals("3a")) opcode="GASPRICE";
		if(bytecode.equals("3b")) opcode="EXTCODESIZE";
		if(bytecode.equals("3c")) opcode="EXTCODECOPY";
		if(bytecode.equals("3d")) opcode="RETURNDATASIZE";
		if(bytecode.equals("3e")) opcode="RETURNDATACOPY";
		if(bytecode.equals("40")) opcode="BLOCKHASH";
		if(bytecode.equals("41")) opcode="COINBASE";
		if(bytecode.equals("42")) opcode="TIMESTAMP";
		if(bytecode.equals("43")) opcode="NUMBER";
		if(bytecode.equals("44")) opcode="DIFFICULTY";
		if(bytecode.equals("45")) opcode="GASLIMIT";
		if(bytecode.equals("50")) opcode="POP";
		if(bytecode.equals("51")) opcode="MLOAD";
		if(bytecode.equals("52")) opcode="MSTORE";
		if(bytecode.equals("53")) opcode="MSTORE8";
		if(bytecode.equals("54")) opcode="SLOAD";
		if(bytecode.equals("55")) opcode="SSTORE";
		if(bytecode.equals("56")) opcode="JUMP";
		if(bytecode.equals("57")) opcode="JUMPI";
		if(bytecode.equals("58")) opcode="PC";
		if(bytecode.equals("59")) opcode="MSIZE";
		if(bytecode.equals("5a")) opcode="GAS";
		if(bytecode.equals("5b")) opcode="JUMPDEST";
		if(bytecode.equals("60")) opcode="PUSH1";
		if(bytecode.equals("61")) opcode="PUSH2";
		if(bytecode.equals("62")) opcode="PUSH3";
		if(bytecode.equals("63")) opcode="PUSH4";
		if(bytecode.equals("64")) opcode="PUSH5";
		if(bytecode.equals("65")) opcode="PUSH6";
		if(bytecode.equals("66")) opcode="PUSH7";
		if(bytecode.equals("67")) opcode="PUSH8";
		if(bytecode.equals("68")) opcode="PUSH9";
		if(bytecode.equals("69")) opcode="PUSH10";
		if(bytecode.equals("6A")) opcode="PUSH11";
		if(bytecode.equals("6B")) opcode="PUSH12";
		if(bytecode.equals("6C")) opcode="PUSH13";
		if(bytecode.equals("6D")) opcode="PUSH14";
		if(bytecode.equals("6E")) opcode="PUSH15";
		if(bytecode.equals("6F")) opcode="PUSH16";
		if(bytecode.equals("70")) opcode="PUSH17";
		if(bytecode.equals("71")) opcode="PUSH18";
		if(bytecode.equals("72")) opcode="PUSH19";
		if(bytecode.equals("73")) opcode="PUSH20";
		if(bytecode.equals("74")) opcode="PUSH21";
		if(bytecode.equals("75")) opcode="PUSH22";
		if(bytecode.equals("76")) opcode="PUSH23";
		if(bytecode.equals("77")) opcode="PUSH24";
		if(bytecode.equals("78")) opcode="PUSH25";
		if(bytecode.equals("79")) opcode="PUSH26";
		if(bytecode.equals("7A")) opcode="PUSH27";
		if(bytecode.equals("7B")) opcode="PUSH28";
		if(bytecode.equals("7C")) opcode="PUSH29";
		if(bytecode.equals("7D")) opcode="PUSH30";
		if(bytecode.equals("7E")) opcode="PUSH31";
		if(bytecode.equals("7F")) opcode="PUSH32";
		if(bytecode.equals("80")) opcode="DUP1";
		if(bytecode.equals("81")) opcode="DUP2";
		if(bytecode.equals("82")) opcode="DUP3";
		if(bytecode.equals("83")) opcode="DUP4";
		if(bytecode.equals("84")) opcode="DUP5";
		if(bytecode.equals("85")) opcode="DUP6";
		if(bytecode.equals("86")) opcode="DUP7";
		if(bytecode.equals("87")) opcode="DUP8";
		if(bytecode.equals("88")) opcode="DUP9";
		if(bytecode.equals("89")) opcode="DUP10";
		if(bytecode.equals("8A")) opcode="DUP11";
		if(bytecode.equals("8B")) opcode="DUP12";
		if(bytecode.equals("8C")) opcode="DUP13";
		if(bytecode.equals("8D")) opcode="DUP14";
		if(bytecode.equals("8E")) opcode="DUP15";
		if(bytecode.equals("8F")) opcode="DUP16";
		if(bytecode.equals("90")) opcode="SWAP1";
		if(bytecode.equals("91")) opcode="SWAP2";
		if(bytecode.equals("92")) opcode="SWAP3";
		if(bytecode.equals("93")) opcode="SWAP4";
		if(bytecode.equals("94")) opcode="SWAP5";
		if(bytecode.equals("95")) opcode="SWAP6";
		if(bytecode.equals("96")) opcode="SWAP7";
		if(bytecode.equals("97")) opcode="SWAP8";
		if(bytecode.equals("98")) opcode="SWAP9";
		if(bytecode.equals("99")) opcode="SWAP10";
		if(bytecode.equals("9A")) opcode="SWAP11";
		if(bytecode.equals("9B")) opcode="SWAP12";
		if(bytecode.equals("9C")) opcode="SWAP13";
		if(bytecode.equals("9D")) opcode="SWAP14";
		if(bytecode.equals("9E")) opcode="SWAP15";
		if(bytecode.equals("9F")) opcode="SWAP16";
		if(bytecode.equals("a0")) opcode="LOG0";
		if(bytecode.equals("a1")) opcode="LOG1";
		if(bytecode.equals("a2")) opcode="LOG2";
		if(bytecode.equals("a3")) opcode="LOG3";
		if(bytecode.equals("a4")) opcode="LOG4";
		if(bytecode.equals("f0")) opcode="CREATE";
		if(bytecode.equals("f1")) opcode="CALL";
		if(bytecode.equals("f2")) opcode="CALLCODE";
		if(bytecode.equals("f3")) opcode="RETURN";
		if(bytecode.equals("f4")) opcode="DELEGATECALL";
		if(bytecode.equals("fa")) opcode="STATICCALL";
		if(bytecode.equals("fd")) opcode="REVERT";
		if(bytecode.equals("fe")) opcode="INVALID";
		if(bytecode.equals("ff")) opcode="SELFDESTRUCT";
		
		
		return opcode;
	}

}

