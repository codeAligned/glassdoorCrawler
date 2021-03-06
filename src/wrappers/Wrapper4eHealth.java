/**
 *
 */
package wrappers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;



import structures.Post;

/**
 * @author Md Mustafizur Rahman
 * @version 0.1
 * @category Wrapper
 * sample code for parsing html files from eHealth forum (http://ehealthforum.com/health/health_forums.html)
 * and extract threaded discussions to json format
 */
public class Wrapper4eHealth extends WrapperBase {
   
    //TODO: You need to extend this wrapper to deal with threaded discussion across multiple pages
    // 1. get the right "next page"
    // 2. avoid any duplication
    // 3. extract the right reply-to relation when across pages
   
	PrintWriter questionWriter;
	PrintWriter experienceWriter;
	PrintWriter csvWriter;
	
    public Wrapper4eHealth(String category) {
        super();
       
        
        
        try{
        	questionWriter = new PrintWriter(new File("./data/interviewQuestions/"+category+"/questions.txt"));
        	experienceWriter = new PrintWriter(new File("./data/interviewQuestions/"+category+"/experience.txt"));
        	csvWriter = new PrintWriter(new File("./data/interviewQuestions/"+category+"/allThings.csv"));
		}catch(Exception e){
			System.err.println("File Not found!!");
		}
       
        
        m_dateParser = new SimpleDateFormat("MMMM dd, yyyy");//Date format in this forum: April 20th, 2012
       
    }
   
    protected String parseDate(Element dateElm) throws ParseException {
        String text = dateElm.text();
        if (text.contains("replied "))
            text = text.substring(8);       
       
        //decode the descriptive date [st, nd, rd, th]
        if (text.contains("st,"))
            text = text.replace("st,", ",");
        else if (text.contains("nd,"))
            text = text.replace("nd,", ",");
        else if (text.contains("rd,"))
            text = text.replace("rd,", ",");
        else if (text.contains("th,"))
            text = text.replace("th,", ",");
       
        return super.parseDate(text);
    }
   
    @SuppressWarnings("unused")
	private Post extractPost(Element elm) {
        Elements tmpElms = elm.getElementsByAttributeValue("name", "quick_reply_input");
        if (tmpElms==null || !tmpElms.isEmpty())
            return null;
        
        if(elm.getElementsByClass("vt_post_body").first().text().toString().contains("This post has been removed"))
        {
        	System.out.println("This post has been removed".toUpperCase());
        	Post p = null;
            return p;
        }
        
        else{
        
        Element tmpElmA, tmpElmB;
       
        //get post ID
        tmpElmA = elm.getElementsByClass("vt_usefull_post_form_holder").first();
       
       
        if(tmpElmA!=null)
        {   
        tmpElmB = tmpElmA.getElementsByTag("input").first();
       
        Post p = new Post(tmpElmB.attr("id"));
       
        //get timestamp of this post
        tmpElms = elm.getElementsByClass("vt_reply_timestamp");           
        if (tmpElms != null && !tmpElms.isEmpty()) {
            try {
                p.setDate(parseDate(tmpElms.first()));
               
                //get replyToID
                //Change here
                //if(elm.attr("style")!=null)
                p.setReplyToID(extractReplyToID(elm.attr("style")));
                p.setLevel(extractLevel(elm.attr("style")));
            } catch (ParseException e) {
                System.err.format("[Error]Failed to parse date in %s!\n", p.getID());
                e.printStackTrace();
            }
        } else {// no re-occurrence of first post in eHealth
            tmpElmA = elm.getElementsByClass("vt_first_timestamp").first();
            try {
                p.setDate(parseDate(tmpElmA));
            } catch (ParseException e) {
                System.err.println("[Error]Failed to parse date for the first post!\n");
                e.printStackTrace();
            }
        }
       
        //get author information
        tmpElmA = elm.getElementsByClass("vt_asked_by_user").first();
        tmpElmB = tmpElmA.getElementsByTag("a").first();
        if(tmpElmB != null)
            {
                p.setAuthorID(tmpElmB.attr("href"));   
                p.setAuthor(tmpElmB.text());
                   
            }
           
        //get post title
        tmpElmA = elm.getElementsByClass("vt_message_subject").first();
        if (tmpElmA != null) // first post's title is thread's title
            p.setTitle(tmpElmA.text());
       
        //get post content
        tmpElmA = elm.getElementsByClass("vt_post_body").first();
        p.setContent(tmpElmA.text());
        //System.out.println(tmpElmA.text());
        return p;
        }
        else
        {
            Post p = null;
            return p;
        }
      }
    }
   
    private int extractLevel(String text){
        int start = 13, end = text.indexOf("px;", start);//fixed format for padding, starts with "padding-left:"
        if (end==start)
            return 0;
        else
            return Integer.valueOf(text.substring(start, end))/50;       
    }

       
    // interviewReviewDetails
    // interviewQuestions
    
    @Override
    protected boolean parseHTML(Document doc) {     
    	
    	csvWriter.println("Date, Experience, Questions");
    	//Element interviewDetails = 
    	int size = doc.getElementsByClass("interviewDetails").size();
    	for(int i=0; i<size;i++){
    		String date = doc.getElementsByTag("time").get(i).attr("datetime");
    		String datestr = doc.getElementsByTag("time").get(i).text();
    		//System.out.println(date);
    		//System.out.println(datestr);
    		Element interviewDetails = doc.getElementsByClass("interviewDetails").get(i);
    		//System.out.println(interviewDetails.text());
    		Element interviewques = null;
    		String ques = "No Question";
    		if(i<doc.getElementsByClass("interviewQuestions").size()){
    			interviewques = doc.getElementsByClass("interviewQuestions").get(i);
    			ques = interviewques.text().replaceAll("[^a-zA-Z0-9. ]", " ").toLowerCase();
    			//System.out.println(interviewques.text());

    			questionWriter.println(datestr);
    			questionWriter.println("---------------");
    			questionWriter.println(interviewques.text()+"\n");
    		}
    		experienceWriter.println(datestr);
    		experienceWriter.println("---------------");
    		experienceWriter.println(interviewDetails.text()+"\n");
    		if(i<doc.getElementsByClass("interviewQuestions").size()){
    			experienceWriter.println(interviewques.text()+"\n");
    		}
    		else{
    			experienceWriter.println(ques);
    		}
    		
    		//String exp = interviewDetails.text().replaceAll("(\\r|\\n|\\r\\n)+", " ");
    		//String ques = interviewques.text().replaceAll("(\\r|\\n|\\r\\n)+", " ");
    		
    		String exp = interviewDetails.text().replaceAll("[^a-zA-Z0-9. ]", " ").toLowerCase();
    		
    		csvWriter.println(date+","+exp+","+ques);
    		
    	}
       
    	
    	
       return !m_posts.isEmpty();
    }
   
    
    public void closeWriter(){
    	questionWriter.flush();
    	experienceWriter.flush();
    	csvWriter.flush();
    	
    	questionWriter.close();
    	experienceWriter.close();
    	csvWriter.close();
    	
    }
    
    @Override
    protected String extractReplyToID(String text) {
        int level = extractLevel(text);
        if (level == 0)
            return m_posts.get(0).getID();
        else {
            Post p = null;
            for(int i=m_posts.size()-1; i>=0; i--) {
                p = m_posts.get(i);
                if (p.getLevel()==level-1)
                    break;
            }
            return p.getID();
        }
    }

    public static void main(String[] args) {
        
    	String company = "dropbox";
    	
    	Wrapper4eHealth wrapper = new Wrapper4eHealth(company);
        int range = 0;
        String fileURL = "";
    	int i = 1;
        if(company.equalsIgnoreCase("microsoft"))
        {
        	fileURL = "http://www.glassdoor.com/Interview/Microsoft-Software-Development-Engineer-Interview-Questions-EI_IE1651.0,9_KO10,39_IP";
            range = 79;
        }
    	else if(company.equalsIgnoreCase("amazon"))
    	{
    		fileURL = "http://www.glassdoor.com/Interview/Amazon-com-Software-Development-Engineer-Interview-Questions-EI_IE6036.0,10_KO11,40_IP";
            range = 93;
    	}
    	else if(company.equalsIgnoreCase("oracle"))
    	{
    		fileURL = "http://www.glassdoor.com/Interview/Oracle-Software-Engineer-Interview-Questions-EI_IE1737.0,6_KO7,24_IP";
        	range = 14;
    	}
    	else if(company.equalsIgnoreCase("cisco"))
    	{
    		fileURL = "http://www.glassdoor.com/Interview/Cisco-Systems-Software-Engineer-Interview-Questions-EI_IE1425.0,13_KO14,31_IP";
        	range = 30;
    	}
    	else if(company.equalsIgnoreCase("ciscointern"))
    	{
    		fileURL = "http://www.glassdoor.com/Interview/Cisco-Systems-Intern-Interview-Questions-EI_IE1425.0,13_KO14,20_IP";
        	range = 6;
    	}
    	else if(company.equalsIgnoreCase("google"))
    	{
    		fileURL = "http://www.glassdoor.com/Interview/Google-Software-Engineer-Interview-Questions-EI_IE9079.0,6_KO7,24_IP";
        	range = 153;
    	}
    	else if(company.equalsIgnoreCase("googleintern"))
    	{
    		fileURL = "http://www.glassdoor.com/Interview/Google-Software-Engineer-Intern-Interview-Questions-EI_IE9079.0,6_KO7,http://www.glassdoor.com/Interview/Google-Software-Engineer-Intern-Interview-Questions-EI_IE9079.0,6_KO7,31_IP";
        	range = 13;
    	}
    	else if(company.equalsIgnoreCase("facebook"))
    	{
    	
    		fileURL = "http://www.glassdoor.com/Interview/Facebook-Software-Engineer-Interview-Questions-EI_IE40772.0,8_KO9,26_IP";
    		range = 59;
    	}
    	else if(company.equalsIgnoreCase("visa"))
    	{
    	
    	 	fileURL = "http://www.glassdoor.com/Interview/Visa-Inc-Software-Engineer-Interview-Questions-EI_IE3035.0,8_KO9,26_IP";
        	range = 4;
    	}
    	else if(company.equalsIgnoreCase("intel"))
    	{
    	
    	 	fileURL = "http://www.glassdoor.com/Interview/Intel-Corporation-Software-Engineer-Interview-Questions-EI_IE1519.0,17_KO18,35_IP";
        	range = 18;
    	}
    	else if(company.equalsIgnoreCase("uber"))
    	{
    	
    	 	fileURL = "http://www.glassdoor.com/Interview/Uber-Software-Engineer-Interview-Questions-EI_IE575263.0,4_KO5,22_IP";
        	range = 10;
    	}
        
    	else if(company.equalsIgnoreCase("dropbox"))
    	{
    	
    	 	fileURL = "http://www.glassdoor.com/Interview/Dropbox-Software-Engineer-Interview-Questions-EI_IE415350.0,7_KO8,25_IP";
        	range = 7;
    	}
        
        
        for(i=1;i<=range;i=i+1)
        {
        	String fileLink = fileURL+i+".htm";
        	String filename = "./data/glassdoor/"+company+"/"+i+".htm";
            filedown.download1(fileLink,filename);
            wrapper.parseHTML(filename);
        }
        
        wrapper.closeWriter();

    }

}


class url_recorder
{
    static ArrayList<String> check_url=new ArrayList<String>();;
   
   
}

