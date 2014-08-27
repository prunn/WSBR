package com.prunn.rfdynhud.widgets.prunn.wsbr.racegap;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.Random;

import net.ctdp.rfdynhud.gamedata.LiveGameData;
import net.ctdp.rfdynhud.gamedata.ScoringInfo;
import net.ctdp.rfdynhud.gamedata.VehicleScoringInfo;
import net.ctdp.rfdynhud.properties.BooleanProperty;
import net.ctdp.rfdynhud.properties.ColorProperty;
import net.ctdp.rfdynhud.properties.DelayProperty;
import net.ctdp.rfdynhud.properties.ImagePropertyWithTexture;
import net.ctdp.rfdynhud.properties.IntProperty;
import net.ctdp.rfdynhud.properties.PropertiesContainer;
import net.ctdp.rfdynhud.properties.PropertyLoader;
import net.ctdp.rfdynhud.render.DrawnString;
import net.ctdp.rfdynhud.render.DrawnString.Alignment;
import net.ctdp.rfdynhud.render.DrawnStringFactory;
import net.ctdp.rfdynhud.render.TextureImage2D;
import net.ctdp.rfdynhud.util.NumberUtil;
import net.ctdp.rfdynhud.util.PropertyWriter;
import net.ctdp.rfdynhud.util.SubTextureCollector;
import net.ctdp.rfdynhud.util.TimingUtil;
import net.ctdp.rfdynhud.valuemanagers.Clock;
import net.ctdp.rfdynhud.values.FloatValue;
import net.ctdp.rfdynhud.widgets.base.widget.Widget;
import com.prunn.rfdynhud.widgets.prunn._util.PrunnWidgetSet;
import com.prunn.rfdynhud.widgets.prunn.wsbr.raceinfos.RaceInfosWidget;

/**
 * 
 * 
 * @author Prunn 2011
 */


public class RaceGapWidget extends Widget
{
    private static final Random rnd = new Random( System.nanoTime() );
    
    private final ImagePropertyWithTexture imgPos = new ImagePropertyWithTexture( "imgPos", "prunn/wsbr/wsbrpos.png" );
    private final ImagePropertyWithTexture imgNumber = new ImagePropertyWithTexture( "imgNumber", "prunn/wsbr/wsbrnumber.png" );
    private final ImagePropertyWithTexture imgName = new ImagePropertyWithTexture( "imgName", "prunn/wsbr/wsbrname.png" );
    private final ImagePropertyWithTexture imgTeam = new ImagePropertyWithTexture( "imgTeam", "prunn/wsbr/wsbrteam.png" );
    private final ImagePropertyWithTexture imgTime = new ImagePropertyWithTexture( "imgTime", "prunn/wsbr/wsbrlaptime.png" );
    private final ImagePropertyWithTexture imgLastGap = new ImagePropertyWithTexture( "imgTime", "prunn/wsbr/wsbrgapyellow.png" );
    private final ImagePropertyWithTexture imgTitle = new ImagePropertyWithTexture( "imgTitle", "prunn/wsbr/wsbrwinner.png" );
    
    private final ColorProperty fontColor2 = new ColorProperty( "fontColor2", PrunnWidgetSet.FONT_COLOR2_NAME );
    
    private final IntProperty frequency = new IntProperty( "appearenceFrequency", "frequency", 3 );
    private final BooleanProperty showTeams = new BooleanProperty( "showTeams", "showTeams", true);
    //private final BooleanProperty showPastGap = new BooleanProperty( "showPastGap", "showPastGap", true);
    
    private final DelayProperty visibleTime = new DelayProperty( "visibleTime", DelayProperty.DisplayUnits.SECONDS, 15 );
    private long visibleEnd = 0;
    
    private DrawnString dsPos = null;
    private DrawnString dsPos2 = null;
    private DrawnString dsNumber = null;
    private DrawnString dsNumber2 = null;
    private DrawnString dsName = null;
    private DrawnString dsName2 = null;
    private DrawnString dsTeam = null;
    private DrawnString dsTeam2 = null;
    private DrawnString dsTime = null;
    //private DrawnString dsLastGap = null;
    
    private final FloatValue gapFront = new FloatValue( -1f, 0.001f );
    private final FloatValue gapBehind = new FloatValue( -1f, 0.001f );
    private final FloatValue currentSector = new FloatValue( -1f, 1f );
    //private float lastGapFront = 0.000f;
    //private float lastGapBehind = 0.000f;
    private final FloatValue changedIDFront = new FloatValue( -1f, 0.001f );
    private final FloatValue changedIDBehind = new FloatValue( -1f, 0.001f );
    
    private RaceInfosWidget raceInfosWidget = null;
    
    
    @Override
    public void onCockpitEntered( LiveGameData gameData, boolean isEditorMode )
    {
        super.onCockpitEntered( gameData, isEditorMode );
        //visibleEnd = Long.MAX_VALUE;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void initSubTextures( LiveGameData gameData, boolean isEditorMode, int widgetInnerWidth, int widgetInnerHeight, SubTextureCollector collector )
    {
     
    }
    
    @Override
    protected void initialize( LiveGameData gameData, boolean isEditorMode, DrawnStringFactory drawnStringFactory, TextureImage2D texture, int width, int height )
    {
        int fh = TextureImage2D.getStringHeight( "0yI", getFontProperty() );
        int rowHeight = height / 3;
        
        int fieldWidth1 = rowHeight;
        int fieldWidth2 = Math.round( width * 0.28f ) + fieldWidth1;
        int fieldWidth3 = width - ( fieldWidth1 + fieldWidth2 ) * 2;
        
        imgPos.updateSize( fieldWidth1, rowHeight, isEditorMode );
        imgNumber.updateSize( fieldWidth1, rowHeight, isEditorMode );
        imgName.updateSize( Math.round( width * 0.28f ), rowHeight, isEditorMode );
        imgTeam.updateSize( fieldWidth2, rowHeight, isEditorMode );
        imgTime.updateSize( fieldWidth3, rowHeight, isEditorMode );
        imgLastGap.updateSize( fieldWidth3, rowHeight, isEditorMode );
        imgTitle.updateSize( fieldWidth3, rowHeight, isEditorMode );
        
        changedIDFront.update( 0 );
        changedIDFront.setUnchanged();
        changedIDBehind.update( 0 ); 
        changedIDBehind.setUnchanged();
        
        Color blackFontColor = getFontColor();
        Color whiteFontColor = fontColor2.getColor();
        
        int top1 = rowHeight * 1 + ( rowHeight - fh ) / 2;
        int top2 = rowHeight * 2 + ( rowHeight - fh ) / 2;
        dsPos = drawnStringFactory.newDrawnString( "dsPos", fieldWidth1 / 2, top1, Alignment.CENTER, false, getFont(), isFontAntiAliased(), whiteFontColor );
        dsNumber = drawnStringFactory.newDrawnString( "dsNumber", fieldWidth1 + fieldWidth2 - fieldWidth1 / 2, top1, Alignment.CENTER, false, getFont(), isFontAntiAliased(), blackFontColor );
        dsName = drawnStringFactory.newDrawnString( "dsName", fieldWidth1 + 10, top1, Alignment.LEFT, false, getFont(), isFontAntiAliased(), blackFontColor );
        dsTeam = drawnStringFactory.newDrawnString( "dsTeam", fieldWidth1 + 10, top2, Alignment.LEFT, false, getFont(), isFontAntiAliased(), blackFontColor );
        dsTime = drawnStringFactory.newDrawnString( "dsTime", fieldWidth1 + fieldWidth2 + fieldWidth3 * 9 / 12, top1, Alignment.RIGHT, false, getFont(), isFontAntiAliased(), whiteFontColor );
        //dsLastGap = drawnStringFactory.newDrawnString( "dsLastGap", fieldWidth1 + fieldWidth2 + fieldWidth3 * 6 / 12, top2, Alignment.CENTER, false, getFont(), isFontAntiAliased(), blackFontColor );
        dsPos2 = drawnStringFactory.newDrawnString( "dsPos2", width - fieldWidth1 / 2, top1, Alignment.CENTER, false, getFont(), isFontAntiAliased(), whiteFontColor );
        dsNumber2 = drawnStringFactory.newDrawnString( "dsNumber2", width - fieldWidth1 + fieldWidth2 - fieldWidth1 / 2, top1, Alignment.CENTER, false, getFont(), isFontAntiAliased(), blackFontColor );
        dsName2 = drawnStringFactory.newDrawnString( "dsName2", width - fieldWidth1 - 10, top1, Alignment.RIGHT, false, getFont(), isFontAntiAliased(), blackFontColor );
        dsTeam2 = drawnStringFactory.newDrawnString( "dsTeam2", width - fieldWidth1 - 10, top2, Alignment.RIGHT, false, getFont(), isFontAntiAliased(), blackFontColor );
        
        //raceInfosWidget = getConfiguration().getWidgetByClass( RaceInfosWidget.class, false );
        raceInfosWidget = PrunnWidgetSet.getWidgetByClass( RaceInfosWidget.class, false, getConfiguration() );
    }
    
    @Override
    protected Boolean updateVisibility( LiveGameData gameData, boolean isEditorMode )
    {
        super.updateVisibility( gameData, isEditorMode );
        
        ScoringInfo scoringInfo = gameData.getScoringInfo();
        VehicleScoringInfo viewedVSI = scoringInfo.getViewedVehicleScoringInfo();
        
        currentSector.update( viewedVSI.getSector() );
        
        if(scoringInfo.getLeadersVehicleScoringInfo().getLapsCompleted() < 1 || viewedVSI.getFinishStatus().isFinished())
            return false;
        
        if( viewedVSI.getNextInFront( false ) != null )
            changedIDFront.update( viewedVSI.getNextInFront( false ).getDriverId() );
        
        if( viewedVSI.getNextBehind( false ) != null  )
            changedIDBehind.update( viewedVSI.getNextBehind( false ).getDriverId() );
        
        
        if(isEditorMode)
            return true;
        
        if((raceInfosWidget != null) && raceInfosWidget.isVisible())
            return false;
        
        
        if(scoringInfo.getSessionNanos() < visibleEnd)
            return true;
        
        if( currentSector.hasChanged() && scoringInfo.getNumVehicles() > 1)
        {
            /*if(!changedIDFront.hasChanged())
                lastGapFront = gapFront.getValue();
            else
                lastGapFront = 0f;
            
            if(!changedIDBehind.hasChanged())
                lastGapBehind = gapBehind.getValue();
            else
                lastGapBehind = 0f;*/
            
            
            if( (int)(rnd.nextFloat()*frequency.getValue()) == 0 )
            {
                if(viewedVSI.getNextInFront( false ) != null)
                    gapFront.update( Math.abs( viewedVSI.getTimeBehindNextInFront( false )) );
                if(viewedVSI.getNextBehind( false ) != null)
                    gapBehind.update( Math.abs( viewedVSI.getNextBehind( false ).getTimeBehindNextInFront( false ) ));
                
                forceCompleteRedraw( true );
                visibleEnd = scoringInfo.getSessionNanos() + visibleTime.getDelayNanos();
                return true;
            }
            
        }
        return false;
    		
    }
    @Override
    protected void drawBackground( LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height, boolean isRoot )
    {
        super.drawBackground( gameData, isEditorMode, texture, offsetX, offsetY, width, height, isRoot );
        
        texture.clear( imgPos.getTexture(), offsetX, offsetY + imgPos.getTexture().getWidth(), false, null );
        texture.clear( imgName.getTexture(), offsetX + imgPos.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth(), false, null );
        if(showTeams.getValue())
            texture.clear( imgTeam.getTexture(), offsetX + imgPos.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth() + height / 3, false, null );
        
        texture.clear( imgNumber.getTexture(), offsetX + imgPos.getTexture().getWidth() + imgName.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth(), false, null );
        
        texture.clear( imgTime.getTexture(), offsetX + imgPos.getTexture().getWidth() + imgTeam.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth(), false, null );
        
        
        
        //if ( (( gapBehind.getValue() < gapFront.getValue() && lastGapBehind > 0) || ( gapBehind.getValue() > gapFront.getValue() && lastGapFront > 0)) && showPastGap.getValue())
        //    texture.clear( imgLastGap.getTexture(), offsetX + imgPos.getTexture().getWidth() + imgTeam.getTexture().getWidth(), offsetY + 2*imgPos.getTexture().getWidth(), false, null );
        
        texture.clear( imgNumber.getTexture(), offsetX + imgPos.getTexture().getWidth() + imgTeam.getTexture().getWidth() + imgTime.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth(), false, null );
        if(showTeams.getValue())
            texture.clear( imgTeam.getTexture(), offsetX + imgPos.getTexture().getWidth() + imgTeam.getTexture().getWidth() + imgTime.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth() + height / 3, false, null );
        
        texture.clear( imgName.getTexture(), offsetX  + imgPos.getTexture().getWidth()*2 + imgTeam.getTexture().getWidth() + imgTime.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth(), false, null );
        texture.clear( imgPos.getTexture(), offsetX + width - imgPos.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth() , false, null );
    }
    
    
    @Override
    protected void drawWidget( Clock clock, boolean needsCompleteRedraw, LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height )
    {
        
        if ( needsCompleteRedraw || (clock.c() && ( gapBehind.hasChanged() || gapFront.hasChanged() ) ) )
        {
            VehicleScoringInfo vsi1;
            VehicleScoringInfo vsi2;
            VehicleScoringInfo viewedvsi = gameData.getScoringInfo().getViewedVehicleScoringInfo();
            String place, place2, name, name2, team, team2, number, number2;
            String gap;
            //float lastgap;
            
            if(viewedvsi.getNextBehind( false ) == null)
            {
                vsi1 = viewedvsi.getNextInFront( false );
                vsi2 = viewedvsi;
                if(viewedvsi.getLapsBehindNextInFront(false) == 0)
                    gap = TimingUtil.getTimeAsGapString( gapFront.getValue());
                else
                {
                    String laps = ( viewedvsi.getLapsBehindNextInFront(false) > 1 ) ? " Laps" : " Lap";
                    gap = "+ " + viewedvsi.getLapsBehindNextInFront(false) + laps;
                }
                    //lastgap = LastGapFront;
            }
            else
                if(viewedvsi.getNextInFront( false ) == null)
                {
                    vsi1 = viewedvsi;
                    vsi2 = viewedvsi.getNextBehind( false );
                    if(viewedvsi.getNextBehind( false ).getLapsBehindNextInFront(false) == 0)
                        gap = TimingUtil.getTimeAsGapString( gapBehind.getValue());
                    else
                    {
                        String laps = ( viewedvsi.getNextBehind( false ).getLapsBehindNextInFront(false) > 1 ) ? " Laps" : " Lap";
                        gap = "+ " + viewedvsi.getNextBehind( false ).getLapsBehindNextInFront(false) + laps;
                    }
                }
                else
                    if(viewedvsi.getTimeBehindNextInFront( false ) < viewedvsi.getNextBehind( false ).getTimeBehindNextInFront( false ) || viewedvsi.getLapsBehindNextInFront( false ) < viewedvsi.getNextBehind( false ).getLapsBehindNextInFront( false ))
                    {
                        vsi1 = viewedvsi.getNextInFront( false );
                        vsi2 = viewedvsi;
                        if(viewedvsi.getLapsBehindNextInFront(false) == 0)
                            gap = TimingUtil.getTimeAsGapString( gapFront.getValue());
                        else
                        {
                            String laps = ( viewedvsi.getLapsBehindNextInFront(false) > 1 ) ? " Laps" : " Lap";
                            gap = "+ " + viewedvsi.getLapsBehindNextInFront(false) + laps;
                        }
                            //lastgap = LastGapFront;
                    }
                    else
                    {
                        vsi1 = viewedvsi;
                        vsi2 = viewedvsi.getNextBehind( false );
                        if(viewedvsi.getNextBehind( false ).getLapsBehindNextInFront(false) == 0)
                            gap = TimingUtil.getTimeAsGapString( gapBehind.getValue());
                        else
                        {
                            String laps = ( viewedvsi.getNextBehind( false ).getLapsBehindNextInFront(false) > 1 ) ? " Laps" : " Lap";
                            gap = "+ " + viewedvsi.getNextBehind( false ).getLapsBehindNextInFront(false) + laps;
                        }
                        //lastgap = LastGapBehind;
                    }
            
            //first car
            place = NumberUtil.formatFloat( vsi1.getPlace(getConfiguration().getUseClassScoring()), 0, true);
            name = vsi1.getDriverName();
            if(vsi1.getVehicleInfo() != null)
    	    {
    	        team = vsi1.getVehicleInfo().getFullTeamName();
    	        number = NumberUtil.formatFloat( vsi1.getVehicleInfo().getCarNumber(), 0, true);
    	    }
    	    else
    	    {
    	        team = vsi1.getVehicleClass(); 
                number = NumberUtil.formatFloat( vsi1.getDriverID(), 0, true);
            }
    	    
    	    //second car
    	    place2 = NumberUtil.formatFloat( vsi2.getPlace(getConfiguration().getUseClassScoring()), 0, true);
    	    name2 = vsi2.getDriverName();
            
    	    if(vsi2.getVehicleInfo() != null)
            {
                team2 = vsi2.getVehicleInfo().getFullTeamName();
                number2 = NumberUtil.formatFloat( vsi2.getVehicleInfo().getCarNumber(), 0, true);
            }
            else
            {
                team2 = vsi2.getVehicleClass(); 
                number2 = NumberUtil.formatFloat( vsi2.getDriverID(), 0, true);
            }
    	    
    	    
    	    dsPos.draw( offsetX, offsetY, place, texture );
            dsNumber.draw( offsetX, offsetY, number, texture );
        	dsName.draw( offsetX, offsetY, name, texture );
            if(showTeams.getValue())
                dsTeam.draw( offsetX, offsetY, team, texture );
        	
            dsTime.draw( offsetX, offsetY, gap, texture);
        	
            dsPos2.draw( offsetX, offsetY, place2, texture );
            dsNumber2.draw( offsetX, offsetY, number2, texture );
            dsName2.draw( offsetX, offsetY, name2, texture );
            if(showTeams.getValue())
                dsTeam2.draw( offsetX, offsetY, team2, texture );
            
                
        }
         
    }
    
    
    @Override
    public void saveProperties( PropertyWriter writer ) throws IOException
    {
        super.saveProperties( writer );
        
        writer.writeProperty( fontColor2, "" );
        writer.writeProperty(visibleTime, "");
        writer.writeProperty(frequency, "");
        writer.writeProperty(showTeams, "");
        //writer.writeProperty(showPastGap, "");
    }
    
    @Override
    public void loadProperty( PropertyLoader loader )
    {
        super.loadProperty( loader );
        
        if ( loader.loadProperty( fontColor2 ) );
        else if ( !loader.loadProperty( visibleTime ) );
        else if (! loader.loadProperty( frequency ) );
        else if ( loader.loadProperty( showTeams ) );
        //else if ( loader.loadProperty( showPastGap ) );
    }
    
    @Override
    protected void addFontPropertiesToContainer( PropertiesContainer propsCont, boolean forceAll )
    {
        propsCont.addGroup( "Colors and Fonts" );
        
        super.addFontPropertiesToContainer( propsCont, forceAll );
        
        propsCont.addProperty( fontColor2 );
    }
    
    @Override
    public void getProperties( PropertiesContainer propsCont, boolean forceAll )
    {
        super.getProperties( propsCont, forceAll );
        
        propsCont.addGroup( "Specific" );
        
        propsCont.addProperty( visibleTime );
        propsCont.addProperty( frequency );
        propsCont.addProperty( showTeams );
        //propsCont.addProperty( showPastGap );
    }
    
    @Override
    protected boolean canHaveBorder()
    {
        return ( false );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void prepareForMenuItem()
    {
        super.prepareForMenuItem();
        
        getFontProperty().setFont( "Dialog", Font.PLAIN, 6, false, true );
        
    }
    
    public RaceGapWidget()
    {
        super( PrunnWidgetSet.INSTANCE, PrunnWidgetSet.WIDGET_PACKAGE_WSBR, 76.0f, 19.0f );
        
        getBackgroundProperty().setColorValue( "#00000000" );
        getFontProperty().setFont( PrunnWidgetSet.WSBR_FONT_NAME );
        getFontColorProperty().setColor( PrunnWidgetSet.FONT_COLOR1_NAME );
    }
}
