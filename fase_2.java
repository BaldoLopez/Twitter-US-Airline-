package uoc.exercise;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import uoc.utils.FileUtilities;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.PrintUtil;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.DatatypeProperty;

public class Exercise {

private FileUtilities fileUtilities;
	
	static Logger logger = Logger.getLogger(Exercise.class);
	
	public static final String NSTWITTER = "http://www.semanticweb.org/laia/ontologies/2020/1/Twitter#";
	public static final String GEO = "http://www.w3.org/2003/01/geo/wgs84_pos#";
	public Exercise() {
		super();
		fileUtilities = new FileUtilities();
		
	}
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Exercise app = new Exercise();
		app.run();
	}
	
	private void run() throws IOException {
		List<List<String>> fileContents = null;
		try {
			fileContents = fileUtilities.readFileFromClasspath("data/Tweets.csv");
		} catch (FileNotFoundException e) {
			System.err.println("ERROR: File not found");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("ERROR: I/O error");
			e.printStackTrace();
		}
		if (fileContents == null) {
			return;
		}
		BasicConfigurator.configure();
		Model model = FileManager.get().loadModel(
				"file:src/data/PAC3_2020_03_28.owl");
		
		OntModel om = sectionA(fileContents, model);
	
		OntClass high=om.createClass(NSTWITTER+"StrongConfidence");
		
		sectionB(om);
		
		saveToLocalDrive(om);

	}
	
	private void saveToLocalDrive(Model model) throws IOException
	{
		String fileName = "test.owl";
		FileWriter out = new FileWriter( fileName );
		try {
		    model.write( out, "RDF/XML" );
		}
		finally {
		   try {
		       out.close();
		   }
		   catch (IOException closeException) {
		       // ignore
		   }
		}
	}
	
	/**
	 * Exercici 2 A
	 * @param fileContents
	 * @param model
	 */
	private OntModel sectionA(List<List<String>> fileContents, Model model) {
		OntModel m = createOntModel(model);
		Individual aTweet,userName,airline, qualifier;
		for (List<String> row : fileContents) {	
			//TODO 1 Create Tweet
			aTweet = createIndividual(m, "Tweet","tweet_"+row.get(0));
			if(aTweet != null){	
				//TODO 2
				//create instance Name
				
				userName= createIndividual(m, "Name", "username_"+row.get(7));

				//add has_user_name
				
				addObjectPropertyToIndividual(m, aTweet, "has_user_name", userName);
				
				
				//add Latitude and Longitude
				String[] geoTokens=row.get(11).replace("[", "").replace("]", "").replace(" ", "").split(",");

				aTweet.addProperty(m.getDatatypeProperty(GEO+"lat"),model.createTypedLiteral(geoTokens[0],XSDDatatype.XSDfloat));
				aTweet.addProperty(m.getDatatypeProperty(GEO+"long"),model.createTypedLiteral(geoTokens[1],XSDDatatype.XSDfloat));
					
				//add Airline_sentiment_confidence				
				aTweet.addProperty(m.getDatatypeProperty(NSTWITTER+"has_airline_sentiment_confidence"),model.createTypedLiteral(row.get(2),XSDDatatype.XSDdouble));

				//add Airline 
				airline= createIndividual(m, "Airline","airline_"+row.get(5));
				addObjectPropertyToIndividual(m, aTweet, "has_airline", airline);
							
			}
		}
		
		//Print Tweets
		printTweets(m);
		
		//Print Time > 0.5
		printMoreThan(m, 0.5);
		
		//Print Time < 0.5
		printLessThan(m, 0.5);
		return m;
		
	}
	/**
	 * Create an individual from a given class
	 * @param m the ontological model
	 * @param className the name of the class
	 * @param name the name of the individual
	 * @return an individual of the required class
	 */
	private Individual createIndividual(OntModel m, String className, String name) {
		//TODO 1 create individual	
		OntClass c= m.getOntClass(NSTWITTER+className);
		return c.createIndividual(NSTWITTER+name);

	}
	
	/**
	 * Function add a property with a value, if value is different ""
	 * @param m
	 * @param i
	 * @param property
	 * @param value
	 */
	private void addObjectPropertyToIndividual(OntModel m, Individual i,
			String property, Individual value) {
		i.addProperty(m.getProperty(NSTWITTER+property), value);	
	}	
	
	/**
	 * @param m
	 */
	private void printMoreThan(OntModel m, double limit) {
		int counter=0;
		System.out.println("Instances more than "+ limit);
		//TODO 4
		ExtendedIterator<? extends OntResource> it =
				m. getOntClass(NSTWITTER+"Tweet") . listInstances() ;
		RDFNode individualConfidence;
		while (it. hasNext() ) 
		{
			OntResource ontResource = (OntResource) it. next() ;
			individualConfidence=ontResource.getPropertyValue(m. getProperty(NSTWITTER+"has_airline_sentiment_confidence") ) ;
			if(individualConfidence!=null) 
			{
				if(individualConfidence. asLiteral() . getDouble() > limit )
				{
					counter++;
					System. out. println(" - " + PrintUtil. print(ontResource) ) ;
				}
				
			}
		}
		System. out. println("Total number of tweets with confidence greater than: "+limit+"="+counter) ;
	}
	
	/**
	 * 
[rule1: (?a rdf:type pre:Tweet) , (?a pre:has_airline_sentiment_confidence ?e) , ge(?e,0.7)-> (?a rdf:type pre:Strong_confidence) ]	
	 * @param limit
	 */
	
	private void printLessThan(OntModel m, double limit) {
		int counter=0;
		System.out.println("Instances more than "+ limit);
		//TODO 4
		ExtendedIterator<? extends OntResource> it =
				m. getOntClass(NSTWITTER+"Tweet") . listInstances() ;
		RDFNode individualConfidence;
		while (it. hasNext() ) 
		{
			OntResource ontResource = (OntResource) it. next() ;
			individualConfidence=ontResource.getPropertyValue(m. getProperty(NSTWITTER+"has_airline_sentiment_confidence") ) ;
			if(individualConfidence!=null) 
			{
				if(individualConfidence. asLiteral() . getDouble() < limit )
				{
					counter++;
					System. out. println(" - " + PrintUtil. print(ontResource) ) ;
				}
				
			}
		}
		System. out. println("Total number of tweets with confidence less than: "+limit+"="+counter) ;
	}
	
	/**
	 * @param m
	 */
	private void printTweets(OntModel m) {
		//TODO 3
		System.out.println("Tweets: ");
		int counter=0;
		ExtendedIterator<? extends OntResource> it =
		m. getOntClass(NSTWITTER+"Tweet") . listInstances() ;
		while (it. hasNext() ) {
			counter++;
		OntResource ontResource = (OntResource) it. next() ;
		System. out. println(" - " + PrintUtil. print(ontResource) ) ;
		}
		System. out. println("Total number of tweets: "+counter) ;

		
	}
	

	/**
	 * Exercise 2 B
	 * @param om
	 */
	private void sectionB(OntModel om) {
		List<Rule> rules = Rule.rulesFromURL("file:src/data/Rules.rules");
		//List<Rule> rules = Rule.parseRules("[r1: (?a has_airline_sentiment_confidence ?b) , greaterThan(?b,0.7)-> (?a rdf:type Strong_confidence)]"); 
		Reasoner reasoner2 = new GenericRuleReasoner(rules);
		reasoner2 = reasoner2.bindSchema(om);
		InfModel infmodel = ModelFactory.createInfModel(reasoner2, om);
		printStrongConfidence(infmodel);
	}
	
	private void printStrongConfidence(InfModel m) {
		
		System.out.println("StrongConfidence: ");
		if(m!=null)
			printStatements(m, null, RDF.type, m.getResource(NSTWITTER+"StrongConfidence"));
	
	}
	
	/**
	 * Generate an OntModel
	 * @param model
	 * @return
	 */
	private OntModel createOntModel(Model model) {
		
		ModelMaker maker = ModelFactory.createMemModelMaker();
		OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
		spec.setImportModelMaker(maker);
		OntModel m = ModelFactory.createOntologyModel(spec, model);
		return m;
	}
	
	public static void printStatements(Model m, Resource s, Property p, Resource o) {
		int counter=0;
		if(m!=null && (s!= null || p != null || o!= null)){
		    for (StmtIterator i = m.listStatements(s,p,o); i.hasNext(); ) {
		    	counter++;
		        Statement stmt = i.nextStatement();
		        if(stmt.getObject().isURIResource()){
	        		System.out.println(" - " + PrintUtil.print(stmt));
		        }else if(stmt.getObject().isLiteral()){
		        	System.out.println(" - " + PrintUtil.print(stmt));
		        }
		    }
		}
		System.out.println("Number of Tweets with StongConfidence= "+counter);
	}
}
