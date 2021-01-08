package opt01;

	import java.io.FileNotFoundException;
	import java.io.IOException;
	import java.io.InputStream;
	import java.util.Date;
	import java.util.Properties;

public class OptimizeGetPropertyValues {


	
	public static void main(String[] args) throws IOException{
		String result = "";
		InputStream inputStream;
		
		Properties prop = new Properties();
		String propFileName = "config.properties";
		inputStream = OptimizeGetPropertyValues.class.getClassLoader().getResourceAsStream(propFileName)
		
		if (inputStream != null) {
			prop.load(inputStream);
		} else {
			throw new FileNotFoundException("Property file " + propFileName + " not found.");
		}
		
	}
	
}
