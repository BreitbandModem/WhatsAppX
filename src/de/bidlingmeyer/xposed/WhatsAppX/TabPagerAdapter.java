package de.bidlingmeyer.xposed.WhatsAppX;

import java.util.ArrayList;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class TabPagerAdapter extends FragmentStatePagerAdapter {
	
	PagerActivity context;
	ArrayList<String[]> tabs;
	ArrayList<ChatBubbleFragment> frags;
	String conversationName;
	
  public TabPagerAdapter(FragmentManager fm, PagerActivity context, ArrayList<String[]> tabs, String convName) {
    super(fm);
    conversationName = convName;
    this.context = context;
    this.tabs = tabs;
    frags = new ArrayList<ChatBubbleFragment>();
  }
    
  @Override
  public Fragment getItem(int i) {
	  ChatBubbleFragment f = new ChatBubbleFragment(context, context.getBaseContext(), tabs.get(i)[0], tabs.get(i)[2], conversationName);
	  frags.add(f);
	  return f;
  }
  
  @Override
  public int getCount() {
    return tabs.size();
  }
  
  public ChatBubbleFragment get(int i){
	  return frags.get(i);
  }

  public ChatBubbleFragment getFrag(int k) {
	  String name = tabs.get(k)[0];
	  for(int i=0; i<frags.size(); i++){
			if(frags.get(i).conversationName.equals(name)){
				return frags.get(i);
			}
	  }
	  return null;
  }
  /*
	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		super.destroyItem(container, position, object);
	    FragmentManager manager = ((Fragment) object).getFragmentManager();
	    FragmentTransaction trans = manager.beginTransaction();
	    trans.remove((Fragment) object);
	    trans.commit();
	}*/
  
}
