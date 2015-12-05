package de.bidlingmeyer.xposed.WhatsAppX;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import java.util.HashMap;
import java.util.Map;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.RelativeLayout;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * Adds support for making the background color of group conversation rows
 * grey in color.
 * 
 * Note: this code has been written solely to be performant. This means some
 * code is inlined (read: copy/pasted) that wouldn't normally be, and private
 * classes are used in a cache without getters and setters - amongst other things.
 * This is because we're hooking into android.view.View->setTag(java.lang.Object)
 * which is quite a general method, so we want to be as fast as possible.
 */
public class HighlightContacts implements IXposedHookLoadPackage {
    private static final Map<Object, Drawable> processedTags = new HashMap<Object, Drawable>(); 
    private static final Map<View, View> conversationRows = new HashMap<View, View>();

    /**
     * This is initially called by the Xposed Framework when we register this
     * android application as an Xposed Module.
     */
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        // This method is called once per package, so we only want to apply hooks to WhatsApp.
        if (!"com.whatsapp".equals(lpparam.packageName)) {
            return;
        }

        /*if (!Preferences.hasHighlightGroups()) {
            Utils.debug("Ignoring call to setTag() due to the highlight groups feature being disabled");
            return;
        }*/

        findAndHookMethod("android.view.View", lpparam.classLoader, "setTag", Object.class, new XC_MethodHook() {
            @SuppressWarnings("deprecation")
			@Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final View thisView = (View) param.thisObject;

                View conversationRow = conversationRows.get(thisView);
                if (!conversationRows.containsKey(thisView)) {
                    final View contactPickerViewContainer = (View) ((View) param.thisObject).getParent();
                    if (null == contactPickerViewContainer) {
                        // Doesn't even have a parent.
                        conversationRows.put(thisView, null);
                        return;
                    }

                    conversationRow = (View) contactPickerViewContainer.getParent();
                    if (null == conversationRow || !(conversationRow instanceof RelativeLayout)) {
                        // We require that our conversationRow is a RelativeLayout
                        // (see the overall parent of res/layout/conversations_row.xml).
                        conversationRows.put(thisView, null);
                        return;
                    }

                    conversationRows.put(thisView, conversationRow);
                } else if (null == conversationRow) {
                    return;
                }

                final Object tag = param.args[0];

                if (processedTags.containsKey(tag)) {
                    // For performance, there are no debugging or trace lines here. Let's hope it always works!
                    conversationRow.setBackgroundDrawable(processedTags.get(tag));
                    return;
                }

                // We have never considered what color this conversation should be. Let's figure it out.
                //Utils.debug("Cache miss for " + tag + " so computing the correct color");
                Drawable background = null;
                XSharedPreferences prefs = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "contactColor");
				prefs.makeWorldReadable();
				XSharedPreferences prefs2 = new XSharedPreferences("de.bidlingmeyer.xposed.WhatsAppX", "preferences");
				prefs2.makeWorldReadable();
				int groupColor = 0;
				String number = tag.toString();
				if(prefs2.getBoolean("groupHighlight", false) && number.contains("@g.us")){
					groupColor = prefs2.getInt("groupHighlightColor", 0);
				}
				int plus = number.indexOf("+");
				if(plus >  0){
					number = number.substring(plus+1, number.indexOf("_"));
				}else{
					number = number.split("@")[0];
				}
				int color = prefs.getInt(number, groupColor);
                if (color!=0) {
                    background = new ColorDrawable(color);
                }

                conversationRow.setBackgroundDrawable(background);
                processedTags.put(tag, background);
                //Utils.debug("Set background to " + background + " for " + tag);
            }
        });
    }
}