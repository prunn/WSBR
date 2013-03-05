package com.prunn.rfdynhud.widgets.prunn.wsbr.racecontrol;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;

import net.ctdp.rfdynhud.gamedata.LiveGameData;
import net.ctdp.rfdynhud.gamedata.ScoringInfo;
import net.ctdp.rfdynhud.gamedata.VehicleScoringInfo;
import net.ctdp.rfdynhud.properties.ColorProperty;
import net.ctdp.rfdynhud.properties.DelayProperty;
import net.ctdp.rfdynhud.properties.ImagePropertyWithTexture;
import net.ctdp.rfdynhud.properties.PropertiesContainer;
import net.ctdp.rfdynhud.properties.PropertyLoader;
import net.ctdp.rfdynhud.render.DrawnString;
import net.ctdp.rfdynhud.render.DrawnString.Alignment;
import net.ctdp.rfdynhud.render.DrawnStringFactory;
import net.ctdp.rfdynhud.render.TextureImage2D;
import net.ctdp.rfdynhud.util.PropertyWriter;
import net.ctdp.rfdynhud.util.SubTextureCollector;
import net.ctdp.rfdynhud.valuemanagers.Clock;
import net.ctdp.rfdynhud.values.IntValue;
import net.ctdp.rfdynhud.widgets.base.widget.Widget;
import com.prunn.rfdynhud.widgets.prunn._util.PrunnWidgetSet;

/**
 * 
 * 
 * @author Prunn 2011
 */


public class RaceControlWidget extends Widget
{
    private final ImagePropertyWithTexture imgName = new ImagePropertyWithTexture( "imgName", "prunn/wsbr/wsbrname.png" );
    private final ImagePropertyWithTexture imgTime = new ImagePropertyWithTexture( "imgTime", "prunn/wsbr/wsbrteam.png" );
    
    private final ColorProperty fontColor2 = new ColorProperty( "fontColor2", PrunnWidgetSet.FONT_COLOR2_NAME );
    private final DelayProperty visibleTime = new DelayProperty( "visibleTime", DelayProperty.DisplayUnits.SECONDS, 6 );
    private long visibleEnd = 0;
    
    private DrawnString dsRC = null;
    private DrawnString dsMessage = null;
    
    private IntValue[] penalties = null;
    private final IntValue penTotal = new IntValue();
    private int flaggedDriver = 0;
    
    
    @Override
    public void onRealtimeEntered( LiveGameData gameData, boolean isEditorMode )
    {
        super.onRealtimeEntered( gameData, isEditorMode );
        
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
        int fh = TextureImage2D.getStringHeight( "0%C", getFontProperty() );
        int numveh = gameData.getScoringInfo().getNumVehicles();
        
        int rowHeight = height / 2;
        
        imgName.updateSize( Math.round(width * 0.25f), rowHeight, isEditorMode );
        imgTime.updateSize( width, rowHeight, isEditorMode );
        
        Color blackFontColor = getFontColor();
        Color whiteFontColor = fontColor2.getColor();
        
        dsRC = drawnStringFactory.newDrawnString( "dsRC", 10, rowHeight * 0 + ( rowHeight - fh ) / 2, Alignment.LEFT, false, getFont(), isFontAntiAliased(), blackFontColor );
        dsMessage = drawnStringFactory.newDrawnString( "dsMessage", 10, rowHeight * 1 + ( rowHeight - fh ) / 2, Alignment.LEFT, false, getFont(), isFontAntiAliased(), whiteFontColor );
        penalties = new IntValue[numveh];
        for(int i=0;i < numveh;i++)
        { 
            penalties[i] = new IntValue();
            penalties[i].update(0);
            penalties[i].setUnchanged();
        }
        
    }
    
    @Override
    protected Boolean updateVisibility(LiveGameData gameData, boolean isEditorMode)
    {
        super.updateVisibility(gameData, isEditorMode);
        
        int numveh = gameData.getScoringInfo().getNumVehicles();
        ScoringInfo scoringInfo = gameData.getScoringInfo();
        
        if(isEditorMode)
            return true;
        
        if(scoringInfo.getSessionNanos() < visibleEnd)
            return true;
        
        if(scoringInfo.getSessionType().isRace())
        {
            
            int total=0;
            for(int j=0;j < numveh;j++)
            {
                total += scoringInfo.getVehicleScoringInfo( j ).getNumOutstandingPenalties();
            }
            penTotal.update( total );
           
            if(penTotal.getValue() > penTotal.getOldValue() && penTotal.hasChanged() && penTotal.getValue() > 0)
            {
               visibleEnd = scoringInfo.getSessionNanos() + visibleTime.getDelayNanos();
               return true;
            }
            
            penTotal.hasChanged();
        }
        
        return false;   
    }
    
    @Override
    protected void drawBackground( LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height, boolean isRoot )
    {
        super.drawBackground( gameData, isEditorMode, texture, offsetX, offsetY, width, height, isRoot );
        
        texture.clear( imgName.getTexture(), offsetX, offsetY, false, null );
        texture.clear( imgTime.getTexture(), offsetX, offsetY + height / 2, false, null );
    }
    
    @Override
    protected void drawWidget( Clock clock, boolean needsCompleteRedraw, LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height )
    {
        int numveh = gameData.getScoringInfo().getNumVehicles();
        ScoringInfo scoringInfo = gameData.getScoringInfo();
        
        for(int i=0;i < numveh;i++)
        {
           penalties[i].update( scoringInfo.getVehicleScoringInfo( i ).getNumOutstandingPenalties() );
                        
           if(penalties[i].hasChanged() && penalties[i].getValue() > 0 )
               flaggedDriver = i;
        }
        VehicleScoringInfo vsi = gameData.getScoringInfo().getVehicleScoringInfo( flaggedDriver );
        
        if ( needsCompleteRedraw )
        {
            dsRC.draw( offsetX, offsetY, "Race Control", texture );
            dsMessage.draw( offsetX, offsetY, "Drive Through Penalty awarded to " + vsi.getDriverName(), texture );
        }
    }
    
    @Override
    public void saveProperties( PropertyWriter writer ) throws IOException
    {
        super.saveProperties( writer );
        
        writer.writeProperty( fontColor2, "" );
        writer.writeProperty( visibleTime, "" );
    }
    
    @Override
    public void loadProperty( PropertyLoader loader )
    {
        super.loadProperty( loader );
        
        if ( loader.loadProperty( fontColor2 ) );
        else if ( !loader.loadProperty( visibleTime ) );
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
        
        propsCont.addProperty(visibleTime);
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
    
    public RaceControlWidget()
    {
        super( PrunnWidgetSet.INSTANCE, PrunnWidgetSet.WIDGET_PACKAGE_WSBR, 55.0f, 10.5f );
        
        getBackgroundProperty().setColorValue( "#00000000" );
        getFontProperty().setFont( PrunnWidgetSet.WSBR_FONT_NAME );
        getFontColorProperty().setColor( PrunnWidgetSet.FONT_COLOR1_NAME );
    }
}
