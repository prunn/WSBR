package com.prunn.rfdynhud.widgets.prunn.wsbr.raceinfos;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;

import net.ctdp.rfdynhud.gamedata.Laptime;
import net.ctdp.rfdynhud.gamedata.LiveGameData;
import net.ctdp.rfdynhud.gamedata.ScoringInfo;
import net.ctdp.rfdynhud.gamedata.VehicleScoringInfo;
import net.ctdp.rfdynhud.properties.BooleanProperty;
import net.ctdp.rfdynhud.properties.ColorProperty;
import net.ctdp.rfdynhud.properties.DelayProperty;
import net.ctdp.rfdynhud.properties.ImagePropertyWithTexture;
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
import net.ctdp.rfdynhud.values.BoolValue;
import net.ctdp.rfdynhud.values.FloatValue;
import net.ctdp.rfdynhud.values.IntValue;
import net.ctdp.rfdynhud.widgets.base.widget.Widget;
import com.prunn.rfdynhud.widgets.prunn._util.PrunnWidgetSet;

/**
 * 
 * 
 * @author Prunn 2011
 */


public class RaceInfosWidget extends Widget
{
    private final ImagePropertyWithTexture imgPos = new ImagePropertyWithTexture( "imgPos", "prunn/wsbr/wsbrpos.png" );
    private final ImagePropertyWithTexture imgNumber = new ImagePropertyWithTexture( "imgNumber", "prunn/wsbr/wsbrnumber.png" );
    private final ImagePropertyWithTexture imgName = new ImagePropertyWithTexture( "imgName", "prunn/wsbr/wsbrname.png" );
    private final ImagePropertyWithTexture imgTeam = new ImagePropertyWithTexture( "imgTeam", "prunn/wsbr/wsbrteam.png" );
    private final ImagePropertyWithTexture imgTime = new ImagePropertyWithTexture( "imgTime", "prunn/wsbr/wsbrlaptime.png" );
    private final ImagePropertyWithTexture imgTitle = new ImagePropertyWithTexture( "imgTitle", "prunn/wsbr/wsbrtitle.png" );
    private final ImagePropertyWithTexture imgTitleW = new ImagePropertyWithTexture( "imgTitleW", "prunn/wsbr/wsbrwinner.png" );
    private final ImagePropertyWithTexture imgTitleF = new ImagePropertyWithTexture( "imgTitle", "prunn/wsbr/wsbrgapgreen.png" );
    
    private final ColorProperty fontColor2 = new ColorProperty( "fontColor2", PrunnWidgetSet.FONT_COLOR2_NAME );
    
    private final BooleanProperty showWinner = new BooleanProperty( "showWinner", true );
    private final BooleanProperty showFastest = new BooleanProperty( "showFastestLap", "showFastest", true );
    private final BooleanProperty showPitstop = new BooleanProperty( "showPitstop", true );
    private final BooleanProperty showInfo = new BooleanProperty( "showInfo", true );
    private final DelayProperty visibleTime = new DelayProperty( "visibleTime", DelayProperty.DisplayUnits.SECONDS, 6 );
    private long visibleEnd = Long.MAX_VALUE;
    
    private DrawnString dsPos = null;
    private DrawnString dsNumber = null;
    private DrawnString dsName = null;
    private DrawnString dsTeam = null;
    private DrawnString dsTime = null;
    private DrawnString dsTitle = null;
    
    private final FloatValue sessionTime = new FloatValue( -1f, 0.1f );
    private float timestamp = -1;
    private float endTimestamp = -1;
    private float pitTime = -1;
    private final BoolValue pitting = new BoolValue( false );
    private IntValue cveh = new IntValue();
    private IntValue speed = new IntValue();
    private long visibleEndW;
    private long visibleEndF;
    private final FloatValue racetime = new FloatValue( -1f, 0.1f );
    private float sessionstart = 0f;
    private final BoolValue raceFinished = new BoolValue();
    
    private int widgetPart = 0;//0-info 1-pitstop 2-fastestlap 3-winner
    private final FloatValue fastestLapTime = new FloatValue( -1f, 0.001f );
    
    
    @Override
    public void onRealtimeEntered( LiveGameData gameData, boolean isEditorMode )
    {
        super.onCockpitEntered( gameData, isEditorMode );
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
        int fieldWidth2 = Math.round( width * 0.5f ) + fieldWidth1;
        int fieldWidth3 = width - fieldWidth1 - fieldWidth2;
        
        imgPos.updateSize( fieldWidth1, rowHeight, isEditorMode );
        imgNumber.updateSize( fieldWidth1, rowHeight, isEditorMode );
        imgName.updateSize( fieldWidth2 - fieldWidth1, rowHeight, isEditorMode );
        imgTeam.updateSize( fieldWidth2, rowHeight, isEditorMode );
        imgTime.updateSize( fieldWidth3, rowHeight, isEditorMode );
        imgTitle.updateSize( fieldWidth3, rowHeight, isEditorMode );
        imgTitleF.updateSize( fieldWidth3, rowHeight, isEditorMode );
        imgTitleW.updateSize( fieldWidth3, rowHeight, isEditorMode );
        
        Color blackFontColor = getFontColor();
        Color whiteFontColor = fontColor2.getColor();
        
        int top1 = rowHeight * 0 + ( rowHeight - fh ) / 2;
        int top2 = rowHeight * 1 + ( rowHeight - fh ) / 2;
        int top3 = rowHeight * 2 + ( rowHeight - fh ) / 2;
        
        dsPos = drawnStringFactory.newDrawnString( "dsPos", fieldWidth1 / 2, top2, Alignment.CENTER, false, getFont(), isFontAntiAliased(), whiteFontColor );
        dsNumber = drawnStringFactory.newDrawnString( "dsNumber", fieldWidth1 + fieldWidth2 - fieldWidth1 / 2, top2, Alignment.CENTER, false, getFont(), isFontAntiAliased(), blackFontColor );
        dsName = drawnStringFactory.newDrawnString( "dsName", fieldWidth1 + 10, top2, Alignment.LEFT, false, getFont(), isFontAntiAliased(), blackFontColor );
        dsTeam = drawnStringFactory.newDrawnString( "dsTeam", fieldWidth1 + 10, top3, Alignment.LEFT, false, getFont(), isFontAntiAliased(), blackFontColor );
        dsTime = drawnStringFactory.newDrawnString( "dsTime", fieldWidth1 + fieldWidth2 + fieldWidth3  *9 / 12, top2, Alignment.RIGHT, false, getFont(), isFontAntiAliased(), whiteFontColor );
        dsTitle = drawnStringFactory.newDrawnString( "dsTitle", fieldWidth1 + fieldWidth2 + fieldWidth3 * 6 / 12, top1, Alignment.CENTER, false, getFont(), isFontAntiAliased(), whiteFontColor );
        
    }
    
    @Override
    protected Boolean updateVisibility(LiveGameData gameData, boolean isEditorMode)
    {
        super.updateVisibility(gameData, isEditorMode);
        
        ScoringInfo scoringInfo = gameData.getScoringInfo();
        VehicleScoringInfo viewedVSI = scoringInfo.getViewedVehicleScoringInfo();
            
        cveh.update(viewedVSI.getDriverId());
        pitting.update(viewedVSI.isInPits());
        //fastest lap
        Laptime lt = scoringInfo.getFastestLaptime();
        
        if(lt == null || !lt.isFinished())
            fastestLapTime.update(-1F);
        else
            fastestLapTime.update(lt.getLapTime());
        //winner part
        if(scoringInfo.getLeadersVehicleScoringInfo().getLapsCompleted() < 1)
            sessionstart = scoringInfo.getLeadersVehicleScoringInfo().getLapStartTime();
        if(scoringInfo.getSessionTime() > 0)
            racetime.update( scoringInfo.getSessionTime() - sessionstart );
        
        raceFinished.update(viewedVSI.getFinishStatus().isFinished());
        
        
        if(isEditorMode)
        {
            widgetPart = 2;
            return true;
        }
        
        //carinfo
        if(cveh.hasChanged() && cveh.isValid() && showInfo.getValue())
        {
            forceCompleteRedraw(true);
            visibleEnd = scoringInfo.getSessionNanos() + visibleTime.getDelayNanos();
            widgetPart = 0;
            return true;
        }
        
        if(scoringInfo.getSessionNanos() < visibleEnd )
        {
            forceCompleteRedraw(true);
            widgetPart = 0;
            return true;
        }
        
        //pitstop   
        if( pitting.hasChanged())
        {
            endTimestamp = 0;
            timestamp = 0;
        }
            
        if( pitting.getValue() && showPitstop.getValue() )
        {
            if(scoringInfo.getViewedVehicleScoringInfo().getStintLength() >= 0.6)
                widgetPart = 1;
            else
                widgetPart = 0;
            
            speed.update( (int)scoringInfo.getViewedVehicleScoringInfo().getScalarVelocity());
            if(speed.hasChanged() && speed.getValue() < 1)
            {
                endTimestamp = scoringInfo.getSessionTime();
                timestamp = scoringInfo.getSessionTime();
            }
            forceCompleteRedraw(true);
            return true;
        }
        
        //fastest lap
        if(scoringInfo.getSessionNanos() < visibleEndF && fastestLapTime.isValid())
        {
            widgetPart = 2;
            return true; 
        }
        if(fastestLapTime.hasChanged() && fastestLapTime.isValid() && scoringInfo.getLeadersVehicleScoringInfo().getLapsCompleted() > 1 && showFastest.getValue())
        {
            forceCompleteRedraw(true);
            visibleEndF = scoringInfo.getSessionNanos() + visibleTime.getDelayNanos();
            widgetPart = 2;
            return true;
        }
        
        //winner part
        if(scoringInfo.getSessionNanos() < visibleEndW )
        {
            widgetPart = 3;
            return true;
        }
         
        if(raceFinished.hasChanged() && raceFinished.getValue() && showWinner.getValue() )
        {
            forceCompleteRedraw(true);
            visibleEndW = scoringInfo.getSessionNanos() + visibleTime.getDelayNanos();
            widgetPart = 3;
            return true;
        }
        
        return false;	
    }
    @Override
    protected void drawBackground( LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height, boolean isRoot )
    {
        super.drawBackground( gameData, isEditorMode, texture, offsetX, offsetY, width, height, isRoot );
        
        texture.clear( imgPos.getTexture(), offsetX, offsetY + imgPos.getTexture().getWidth(), false, null );
        texture.clear( imgName.getTexture(), offsetX + imgPos.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth(), false, null );
        texture.clear( imgTeam.getTexture(), offsetX + imgPos.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth() + height / 3, false, null );
        texture.clear( imgNumber.getTexture(), offsetX + imgPos.getTexture().getWidth() + imgName.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth(), false, null );
        
        if( widgetPart == 1 )
        {
            texture.clear( imgTime.getTexture(), offsetX + imgPos.getTexture().getWidth() + imgTeam.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth(), false, null );
            texture.clear( imgTitle.getTexture(), offsetX + imgPos.getTexture().getWidth() + imgTeam.getTexture().getWidth(), offsetY, false, null );
        }
        else
            if( widgetPart == 2 )
            {
                texture.clear( imgTime.getTexture(), offsetX + imgPos.getTexture().getWidth() + imgTeam.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth(), false, null );
                texture.clear( imgTitleF.getTexture(), offsetX + imgPos.getTexture().getWidth() + imgTeam.getTexture().getWidth(), offsetY, false, null );
            }
            else
                if(widgetPart == 3)
                {
                    texture.clear( imgTime.getTexture(), offsetX + imgPos.getTexture().getWidth() + imgTeam.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth(), false, null );
                    texture.clear( imgTitleW.getTexture(), offsetX + imgPos.getTexture().getWidth() + imgTeam.getTexture().getWidth(), offsetY, false, null );
                }
                else
                {
                    texture.clear( offsetX + imgPos.getTexture().getWidth() + imgTeam.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth(), imgTime.getTexture().getWidth(), imgTime.getTexture().getHeight() ,true, null );
                    texture.clear( offsetX + imgPos.getTexture().getWidth() + imgTeam.getTexture().getWidth(), offsetY, imgTitle.getTexture().getWidth(), imgTitle.getTexture().getHeight() ,true, null );
                }
                
    }
    
    
    @Override
    protected void drawWidget( Clock clock, boolean needsCompleteRedraw, LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height )
    {
        ScoringInfo scoringInfo = gameData.getScoringInfo();
    	sessionTime.update(scoringInfo.getSessionTime());
    	
    	if ( needsCompleteRedraw || sessionTime.hasChanged() || fastestLapTime.hasChanged())
        {
    	    String team, name, pos, number,title,time;
            
            if( widgetPart == 1 )
        	{
                VehicleScoringInfo currentcarinfos = gameData.getScoringInfo().getViewedVehicleScoringInfo();
                    	    
                if(currentcarinfos.getVehicleInfo() != null)
                {
                    team = currentcarinfos.getVehicleInfo().getFullTeamName();
                    number = NumberUtil.formatFloat( currentcarinfos.getVehicleInfo().getCarNumber(), 0, true);
                }
                else
                {
                    team = currentcarinfos.getVehicleClass(); 
                    number = NumberUtil.formatFloat( currentcarinfos.getDriverID(), 0, true);
                }
                
        	    if(currentcarinfos.getNumOutstandingPenalties() > 0)
        	        title="PENALTY";
        	    else
        	        title="PIT STOP";
        	    
        	    if(scoringInfo.getViewedVehicleScoringInfo().getScalarVelocity() < 1)
        	    {
        	       endTimestamp = gameData.getScoringInfo().getSessionTime();
        	       pitTime = endTimestamp - timestamp;
        	    }
        	    else
        	        if(scoringInfo.getViewedVehicleScoringInfo().getScalarVelocity() > 1 || pitTime > 0)
        	            pitTime = endTimestamp - timestamp;
        	        else
        	            pitTime = 0;
        	    
        	    name = currentcarinfos.getDriverName();
                pos = NumberUtil.formatFloat( currentcarinfos.getPlace(getConfiguration().getUseClassScoring()), 0, true);
                time = TimingUtil.getTimeAsString(pitTime, false, false, true, false );
            	
            }
        	else
        	    if(widgetPart == 2)
                {
                    VehicleScoringInfo fastcarinfos = gameData.getScoringInfo().getFastestLapVSI();
                
                    if(fastcarinfos.getVehicleInfo() != null)
                    {
                        team = fastcarinfos.getVehicleInfo().getFullTeamName();
                        number = NumberUtil.formatFloat( fastcarinfos.getVehicleInfo().getCarNumber(), 0, true);
                    }
                    else
                    {
                        team = fastcarinfos.getVehicleClass(); 
                        number = NumberUtil.formatFloat( fastcarinfos.getDriverID(), 0, true);
                    }
                    name = fastcarinfos.getDriverName();
                    pos = NumberUtil.formatFloat( fastcarinfos.getPlace(getConfiguration().getUseClassScoring()), 0, true);
                    title = "Fastest Lap";
                    time = TimingUtil.getTimeAsLaptimeString(fastestLapTime.getValue() );
                }
                else
                    if(widgetPart == 3)
                    {
                        VehicleScoringInfo winnercarinfos = gameData.getScoringInfo().getLeadersVehicleScoringInfo();
                        
                        if(winnercarinfos.getVehicleInfo() != null)
                        {
                            team = winnercarinfos.getVehicleInfo().getFullTeamName();
                            number = NumberUtil.formatFloat( winnercarinfos.getVehicleInfo().getCarNumber(), 0, true);
                        }
                        else
                        {
                            team = winnercarinfos.getVehicleClass(); 
                            number = NumberUtil.formatFloat( winnercarinfos.getDriverID(), 0, true);
                        }
                        float laps=0;
                        
                        for(int i=1;i <= winnercarinfos.getLapsCompleted(); i++)
                        {
                            if(winnercarinfos.getLaptime(i) != null)
                                laps = winnercarinfos.getLaptime(i).getLapTime() + laps;
                            else
                            {
                                laps = racetime.getValue();
                                i = winnercarinfos.getLapsCompleted()+1;
                            }
                        } 
                        name = winnercarinfos.getDriverName();
                        pos = NumberUtil.formatFloat( winnercarinfos.getPlace(getConfiguration().getUseClassScoring()), 0, true);
                        title = "Winner";
                        time = TimingUtil.getTimeAsLaptimeString( laps );
                    }
                    else
                    {
                        VehicleScoringInfo currentcarinfos = gameData.getScoringInfo().getViewedVehicleScoringInfo();
                        
                        if(currentcarinfos.getVehicleInfo() != null)
                        {
                            team = currentcarinfos.getVehicleInfo().getFullTeamName();
                            number = NumberUtil.formatFloat( currentcarinfos.getVehicleInfo().getCarNumber(), 0, true);
                        }
                        else
                        {
                            team = currentcarinfos.getVehicleClass(); 
                            number = NumberUtil.formatFloat( currentcarinfos.getDriverID(), 0, true);
                        }
                        time="";
                        title="";
                        name = currentcarinfos.getDriverName();
                        pos = NumberUtil.formatFloat( currentcarinfos.getPlace(getConfiguration().getUseClassScoring()), 0, true);
                        
                    }
        	
        	
        	dsPos.draw( offsetX, offsetY, pos, texture );
            dsNumber.draw( offsetX, offsetY, number, texture );
            dsName.draw( offsetX, offsetY, name, texture );
            dsTeam.draw( offsetX, offsetY, team, texture );
            
            dsTime.draw( offsetX, offsetY, time, texture);
            dsTitle.draw( offsetX, offsetY, title, texture );
        }
         
    }
    
    
    @Override
    public void saveProperties( PropertyWriter writer ) throws IOException
    {
        super.saveProperties( writer );
        
        writer.writeProperty( fontColor2, "" );
        writer.writeProperty(visibleTime, "");
        writer.writeProperty(showWinner, "");
        writer.writeProperty(showFastest, "");
        writer.writeProperty(showPitstop, "");
        writer.writeProperty(showInfo, "");
    }
    
    @Override
    public void loadProperty( PropertyLoader loader )
    {
        super.loadProperty( loader );
        
        if ( loader.loadProperty( fontColor2 ) );
        else if ( !loader.loadProperty( visibleTime ) );
        else if ( loader.loadProperty( showWinner ) );
        else if ( loader.loadProperty( showFastest ) );
        else if ( loader.loadProperty( showPitstop ) );
        else if ( loader.loadProperty( showInfo ) );
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
        propsCont.addProperty( showWinner );
        propsCont.addProperty( showFastest );
        propsCont.addProperty( showPitstop );
        propsCont.addProperty( showInfo );
        
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
    
    public RaceInfosWidget()
    {
        super( PrunnWidgetSet.INSTANCE, PrunnWidgetSet.WIDGET_PACKAGE_WSBR, 50.0f, 20.5f );
        
        getBackgroundProperty().setColorValue( "#00000000" );
        getFontProperty().setFont( PrunnWidgetSet.WSBR_FONT_NAME );
        getFontColorProperty().setColor( PrunnWidgetSet.FONT_COLOR1_NAME );
    }
}
