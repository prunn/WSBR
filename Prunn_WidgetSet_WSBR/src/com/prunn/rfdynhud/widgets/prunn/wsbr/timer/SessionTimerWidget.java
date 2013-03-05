package com.prunn.rfdynhud.widgets.prunn.wsbr.timer;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;

import net.ctdp.rfdynhud.gamedata.GamePhase;
import net.ctdp.rfdynhud.gamedata.LiveGameData;
import net.ctdp.rfdynhud.gamedata.ScoringInfo;
import net.ctdp.rfdynhud.gamedata.SessionLimit;
import net.ctdp.rfdynhud.gamedata.YellowFlagState;
import net.ctdp.rfdynhud.properties.ColorProperty;
import net.ctdp.rfdynhud.properties.ImagePropertyWithTexture;
import net.ctdp.rfdynhud.properties.PropertiesContainer;
import net.ctdp.rfdynhud.properties.PropertyLoader;
import net.ctdp.rfdynhud.render.DrawnString;
import net.ctdp.rfdynhud.render.DrawnString.Alignment;
import net.ctdp.rfdynhud.render.DrawnStringFactory;
import net.ctdp.rfdynhud.render.TextureImage2D;
import net.ctdp.rfdynhud.util.PropertyWriter;
import net.ctdp.rfdynhud.util.SubTextureCollector;
import net.ctdp.rfdynhud.util.TimingUtil;
import net.ctdp.rfdynhud.valuemanagers.Clock;
import net.ctdp.rfdynhud.values.BoolValue;
import net.ctdp.rfdynhud.values.EnumValue;
import net.ctdp.rfdynhud.values.FloatValue;
import net.ctdp.rfdynhud.values.IntValue;
import net.ctdp.rfdynhud.values.StringValue;
import net.ctdp.rfdynhud.widgets.base.widget.Widget;
import com.prunn.rfdynhud.widgets.prunn._util.PrunnWidgetSet;

/**
 * World Series by Renault Lap Counter
 * 
 * @author Prunn
 */
public class SessionTimerWidget extends Widget
{
    private static final String YELLOW_FLAG_FONT_COLOR_NAME = "YflagFontColor";
    
    private final ColorProperty yflagFontColor = new ColorProperty( "yelFlagFontColor", YELLOW_FLAG_FONT_COLOR_NAME );
    
    private final EnumValue<YellowFlagState> SCState = new EnumValue<YellowFlagState>();
    private final IntValue lapsLeft = new IntValue();
    private final BoolValue sectorYellowFlag = new BoolValue();
    private final FloatValue sessionTime = new FloatValue( -1f, 0.1f );
    private final ImagePropertyWithTexture imgBG = new ImagePropertyWithTexture( "imgBG", "prunn/wsbr/laptime.png" );
    private final ImagePropertyWithTexture imgBGYellow = new ImagePropertyWithTexture( "imgBGYellow", "prunn/wsbr/laptimeyellow.png" );
    private final ImagePropertyWithTexture imgBGGreen = new ImagePropertyWithTexture( "imgBGGreen", "prunn/wsbr/laptimegreen.png" );
    private final StringValue strLaptime = new StringValue( "" );
    private DrawnString dsSession = null;
    
    
    
    @Override
    public void onRealtimeEntered( LiveGameData gameData, boolean isEditorMode )
    {
        super.onRealtimeEntered( gameData, isEditorMode );
        
        lapsLeft.reset();
        sectorYellowFlag.reset();
        SCState.reset();
        strLaptime.reset();
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
        imgBG.updateSize( width, height, isEditorMode );
        imgBGYellow.updateSize( width, height, isEditorMode );
        imgBGGreen.updateSize( width, height, isEditorMode );
        int fh = TextureImage2D.getStringHeight( "0%C", getFontProperty() );
        dsSession = drawnStringFactory.newDrawnString( "dsSession", width / 2, ( height - fh ) / 2, Alignment.CENTER, false, getFont(), isFontAntiAliased(), getFontColor() );
    }
    
    @Override
    protected Boolean updateVisibility( LiveGameData gameData, boolean isEditorMode )
    {
        super.updateVisibility( gameData, isEditorMode );
        
        ScoringInfo scoringInfo = gameData.getScoringInfo();
        SCState.update(scoringInfo.getYellowFlagState());
        sectorYellowFlag.update(scoringInfo.getSectorYellowFlag(gameData.getScoringInfo().getViewedVehicleScoringInfo().getSector()));
        
        if((SCState.hasChanged() || sectorYellowFlag.hasChanged()) && !isEditorMode)
            forceCompleteRedraw(true);
        
        if( scoringInfo.getGamePhase() == GamePhase.FORMATION_LAP )
        {
            return false;
        }
        if( scoringInfo.getGamePhase() == GamePhase.STARTING_LIGHT_COUNTDOWN_HAS_BEGUN && scoringInfo.getEndTime() <= scoringInfo.getSessionTime() )
        {
            return false;
        }
        return true;
        
    }
    @Override
    protected void drawBackground( LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height, boolean isRoot )
    {
        super.drawBackground( gameData, isEditorMode, texture, offsetX, offsetY, width, height, isRoot );
        
        if(sectorYellowFlag.getValue())
            texture.clear( imgBGYellow.getTexture(), offsetX, offsetY, false, null );
        else
            if(SCState.getValue() == YellowFlagState.RESUME)
                texture.clear( imgBGGreen.getTexture(), offsetX, offsetY, false, null );
            else
                texture.clear( imgBG.getTexture(), offsetX, offsetY, false, null );
           
    }
    @Override
    protected void drawWidget( Clock clock, boolean needsCompleteRedraw, LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height )
    {
        ScoringInfo scoringInfo = gameData.getScoringInfo();
        
        String strLaptime = "";
        
        if (scoringInfo.getSessionType().isRace() && scoringInfo.getViewedVehicleScoringInfo().getSessionLimit() == SessionLimit.LAPS)
        {
            lapsLeft.update(scoringInfo.getMaxLaps() - scoringInfo.getLeadersVehicleScoringInfo().getLapsCompleted());
            GamePhase gamePhase = scoringInfo.getGamePhase();
            
            if ( needsCompleteRedraw || lapsLeft.hasChanged())
            {
                if(gamePhase == GamePhase.SESSION_OVER)
                    strLaptime = "FINISH";
                else 
                    if(lapsLeft.getValue() == 1)
                        strLaptime = "LAST LAP";
                    else
                        strLaptime = lapsLeft.getValueAsString() + " LAPS";
             }
        }
        else // Test day only
            if(scoringInfo.getSessionType().isTestDay())
                strLaptime = "00:00:00";
            else // any other timed session (Race, Qualify, Practice)
            {
                GamePhase gamePhase = scoringInfo.getGamePhase();
                sessionTime.update(scoringInfo.getSessionTime());
                float endTime = scoringInfo.getEndTime();
                
                if ( needsCompleteRedraw || sessionTime.hasChanged() )
                {
                    if(gamePhase == GamePhase.SESSION_OVER || (endTime <= sessionTime.getValue() ) )
                        strLaptime = "FINISH";
                    else 
                        if(gamePhase == GamePhase.STARTING_LIGHT_COUNTDOWN_HAS_BEGUN && endTime <= sessionTime.getValue())
                            strLaptime = "00:00:00";
                        else
                            strLaptime = TimingUtil.getTimeAsString(endTime - sessionTime.getValue(), true, false);
                }
            }
        
        if(SCState.getValue() != YellowFlagState.NONE && SCState.getValue() != YellowFlagState.RESUME)
            strLaptime += " SC";
        
        if ( strLaptime.length() > 0 )
            this.strLaptime.update( strLaptime );
        
        if ( needsCompleteRedraw || ( clock.c() && this.strLaptime.hasChanged() ) )
        {
            Color drawnFontColor;
            if(sectorYellowFlag.getValue() || (SCState.getValue() != YellowFlagState.NONE && SCState.getValue() != YellowFlagState.RESUME))
                drawnFontColor = yflagFontColor.getColor();
            else
                if(SCState.getValue() == YellowFlagState.RESUME)
                    drawnFontColor = getFontColor();
                else
                    drawnFontColor = getFontColor();
            
            dsSession.draw( offsetX, offsetY, this.strLaptime.getValue(), drawnFontColor, texture );
        }
    }
    
    @Override
    public void saveProperties( PropertyWriter writer ) throws IOException
    {
        super.saveProperties( writer );
        
        writer.writeProperty( yflagFontColor, "Yel flag Font Color" );
    }
    
    @Override
    public void loadProperty( PropertyLoader loader )
    {
        super.loadProperty( loader );
        
        if ( loader.loadProperty( yflagFontColor ) );
        
    }
    
    @Override
    protected void addFontPropertiesToContainer( PropertiesContainer propsCont, boolean forceAll )
    {
        propsCont.addGroup( "Colors and Fonts" );
        
        super.addFontPropertiesToContainer( propsCont, forceAll );
        
        propsCont.addProperty( yflagFontColor );
    }
    
    @Override
    public void getProperties( PropertiesContainer propsCont, boolean forceAll )
    {
        super.getProperties( propsCont, forceAll );
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
    
    public SessionTimerWidget()
    {
        super( PrunnWidgetSet.INSTANCE, PrunnWidgetSet.WIDGET_PACKAGE_WSBR, 15.0f, 5.0f );
        
        getBorderProperty().setBorder( null );
        getFontProperty().setFont( PrunnWidgetSet.WSBR_FONT_NAME );
        getFontColorProperty().setColor( "#FAFAFA" );
    }
}
