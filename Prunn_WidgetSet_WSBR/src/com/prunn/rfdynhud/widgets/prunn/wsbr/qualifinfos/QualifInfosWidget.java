package com.prunn.rfdynhud.widgets.prunn.wsbr.qualifinfos;

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
import net.ctdp.rfdynhud.util.NumberUtil;
import net.ctdp.rfdynhud.util.PropertyWriter;
import net.ctdp.rfdynhud.util.SubTextureCollector;
import net.ctdp.rfdynhud.util.TimingUtil;
import net.ctdp.rfdynhud.valuemanagers.Clock;
import net.ctdp.rfdynhud.values.FloatValue;
import net.ctdp.rfdynhud.values.IntValue;
import net.ctdp.rfdynhud.widgets.base.widget.Widget;
import com.prunn.rfdynhud.widgets.prunn._util.PrunnWidgetSet;
import com.prunn.rfdynhud.widgets.prunn.wsbr.qualtime.QualTimeWidget;

/**
 * 
 * 
 * @author Prunn 2011
 */


public class QualifInfosWidget extends Widget
{
    private final ImagePropertyWithTexture imgPos = new ImagePropertyWithTexture( "imgPos", "prunn/wsbr/wsbrpos.png" );
    private final ImagePropertyWithTexture imgNumber = new ImagePropertyWithTexture( "imgNumber", "prunn/wsbr/wsbrnumber.png" );
    private final ImagePropertyWithTexture imgName = new ImagePropertyWithTexture( "imgName", "prunn/wsbr/wsbrname.png" );
    private final ImagePropertyWithTexture imgTeam = new ImagePropertyWithTexture( "imgTeam", "prunn/wsbr/wsbrteam.png" );
    private final ImagePropertyWithTexture imgTime = new ImagePropertyWithTexture( "imgTime", "prunn/wsbr/wsbrlaptime.png" );
    
    private final ColorProperty fontColor2 = new ColorProperty( "fontColor2", PrunnWidgetSet.FONT_COLOR2_NAME );
    private final DelayProperty visibleTime = new DelayProperty( "visibleTime", DelayProperty.DisplayUnits.SECONDS, 6 );
    private long visibleEnd = Long.MAX_VALUE;
    
    private DrawnString dsPos = null;
    private DrawnString dsNumber = null;
    private DrawnString dsName = null;
    private DrawnString dsTeam = null;
    private DrawnString dsTime = null;
    private DrawnString dsGap = null;
    
    private final FloatValue sessionTime = new FloatValue(-1f, 0.1f);
    private final IntValue cveh = new IntValue();
    
    private QualTimeWidget qualTimeWidget = null;
    
    
    
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
        int rowHeight = height / 3;
        int fh = TextureImage2D.getStringHeight( "0", getFontProperty() );
        
        int fieldWidth1 = rowHeight;
        int fieldWidth2 = ( width / 2 ) + fieldWidth1;
        int fieldWidth3 = width - fieldWidth1 - fieldWidth2;
        
        imgPos.updateSize( rowHeight, rowHeight, isEditorMode );
        imgNumber.updateSize( rowHeight, rowHeight, isEditorMode );
        imgName.updateSize( fieldWidth2 - fieldWidth1, rowHeight, isEditorMode );
        imgTeam.updateSize( fieldWidth2, rowHeight, isEditorMode );
        imgTime.updateSize( fieldWidth3, rowHeight, isEditorMode );
        
        Color blackFontColor = getFontColor();
        Color whiteFontColor = fontColor2.getColor();
        
        int top1 = rowHeight * 1 + ( rowHeight - fh ) / 2;
        int top2 = rowHeight * 2 + ( rowHeight - fh ) / 2;
        dsPos = drawnStringFactory.newDrawnString( "dsPos", fieldWidth1 / 2, top1, Alignment.CENTER, false, getFont(), isFontAntiAliased(), whiteFontColor );
        dsName = drawnStringFactory.newDrawnString( "dsName", fieldWidth1 + 10, top1, Alignment.LEFT, false, getFont(), isFontAntiAliased(), blackFontColor );
        dsNumber = drawnStringFactory.newDrawnString( "dsNumber", fieldWidth1 + fieldWidth2 - ( fieldWidth1 / 2 ), top1, Alignment.CENTER, false, getFont(), isFontAntiAliased(), blackFontColor );
        dsTeam = drawnStringFactory.newDrawnString( "dsTeam", fieldWidth1 + 10, top2, Alignment.LEFT, false, getFont(), isFontAntiAliased(), blackFontColor );
        dsTime = drawnStringFactory.newDrawnString( "dsTime", fieldWidth1 + fieldWidth2 + fieldWidth3 * 10 / 12, top1, Alignment.RIGHT, false, getFont(), isFontAntiAliased(), whiteFontColor );
        dsGap = drawnStringFactory.newDrawnString( "dsGap", fieldWidth1 + fieldWidth2 + fieldWidth3 * 10 / 12, top2, Alignment.RIGHT, false, getFont(), isFontAntiAliased(), whiteFontColor );
        
        //qualTimeWidget = getConfiguration().getWidgetByClass( QualTimeWidget.class, false );
        qualTimeWidget = PrunnWidgetSet.getWidgetByClass( QualTimeWidget.class, false, getConfiguration() );
    }
    
    @Override
    protected Boolean updateVisibility( LiveGameData gameData, boolean isEditorMode )
    {
        super.updateVisibility( gameData, isEditorMode );
        
        ScoringInfo scoringInfo = gameData.getScoringInfo();
        
        cveh.update(scoringInfo.getViewedVehicleScoringInfo().getDriverId());
        
        if(isEditorMode)
        {
            return true;
        }
        
        if((qualTimeWidget != null) && qualTimeWidget.isVisible())
        {
            return false;
        }
        //carinfo
        if(cveh.hasChanged() && cveh.isValid())
        {
            forceCompleteRedraw(true);
            visibleEnd = scoringInfo.getSessionNanos() + visibleTime.getDelayNanos();
            return true;
        }
        
        if(scoringInfo.getSessionNanos() < visibleEnd )
        {
            forceCompleteRedraw(true);
            return true;
        }
        
        //pitstop   
        
            
        if( scoringInfo.getViewedVehicleScoringInfo().isInPits() )
        {
            
            forceCompleteRedraw(true);
            return true;
        }
        
        return false;	
    }
    @Override
    protected void drawBackground( LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height, boolean isRoot )
    {
        super.drawBackground( gameData, isEditorMode, texture, offsetX, offsetY, width, height, isRoot );
        
        VehicleScoringInfo currentcarinfos = gameData.getScoringInfo().getViewedVehicleScoringInfo();
        texture.clear( imgPos.getTexture(), offsetX, offsetY + imgPos.getTexture().getWidth(), false, null );
        texture.clear( imgName.getTexture(), offsetX + imgPos.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth(), false, null );
        texture.clear( imgTeam.getTexture(), offsetX + imgPos.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth() + height / 3, false, null );
        texture.clear( imgNumber.getTexture(), offsetX + imgPos.getTexture().getWidth() + imgName.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth(), false, null );
        
        
        
        if(currentcarinfos.getBestLapTime() > 0)
        {
            if(currentcarinfos.getPlace( false ) > 1)
            { 
                texture.clear( imgTime.getTexture(), offsetX + imgPos.getTexture().getWidth() + imgTeam.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth(), false, null );
                texture.clear( imgTime.getTexture(), offsetX + imgPos.getTexture().getWidth() + imgTeam.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth()*2, false, null );
            
            }
            else
            {
                texture.clear( offsetX + imgPos.getTexture().getWidth() + imgTeam.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth(), imgTime.getTexture().getWidth(), imgTime.getTexture().getHeight() ,true, null );
                texture.clear( imgTime.getTexture(), offsetX + imgPos.getTexture().getWidth() + imgTeam.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth()*2, false, null );
            }
                
        }
        else
        {
            texture.clear( offsetX + imgPos.getTexture().getWidth() + imgTeam.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth(), imgTime.getTexture().getWidth(), imgTime.getTexture().getHeight() ,true, null );
            texture.clear( offsetX + imgPos.getTexture().getWidth() + imgTeam.getTexture().getWidth(), offsetY + imgPos.getTexture().getWidth()*2, imgTime.getTexture().getWidth(), imgTime.getTexture().getHeight() ,true, null );
        }         
    }
    
    
    @Override
    protected void drawWidget( Clock clock, boolean needsCompleteRedraw, LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height )
    {
        ScoringInfo scoringInfo = gameData.getScoringInfo();
    	sessionTime.update(scoringInfo.getSessionTime());
    	
    	if ( needsCompleteRedraw || sessionTime.hasChanged())
        {
    	    String team, name, pos, number,gap,time;
            VehicleScoringInfo currentcarinfos = gameData.getScoringInfo().getViewedVehicleScoringInfo();
            
        	name = currentcarinfos.getDriverName();
            pos = NumberUtil.formatFloat( currentcarinfos.getPlace(getConfiguration().getUseClassScoring()), 0, true);
            
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
        	
            
            if(currentcarinfos.getBestLapTime() > 0)
            {
                if(currentcarinfos.getPlace( false ) > 1)
                { 
                    time = TimingUtil.getTimeAsLaptimeString(currentcarinfos.getBestLapTime() );
                    gap = "+ " +  TimingUtil.getTimeAsLaptimeString( currentcarinfos.getBestLapTime() - gameData.getScoringInfo().getLeadersVehicleScoringInfo().getBestLapTime() );
                }
                else
                {
                    time = "";
                    gap = TimingUtil.getTimeAsLaptimeString(currentcarinfos.getBestLapTime());
                }
                    
            }
            else
            {
                time="";
                gap="";
            }
            
            dsPos.draw( offsetX, offsetY, pos, texture );
            dsNumber.draw( offsetX, offsetY, number, texture );
            dsName.draw( offsetX, offsetY, name, texture );
            dsTeam.draw( offsetX, offsetY, team, texture );
            dsTime.draw( offsetX, offsetY, time, texture);
            dsGap.draw( offsetX, offsetY, gap, texture );
        }
         
    }
    
    
    @Override
    public void saveProperties( PropertyWriter writer ) throws IOException
    {
        super.saveProperties( writer );
        
        writer.writeProperty( fontColor2, "" );
        writer.writeProperty(visibleTime, "");
        
    }
    
    @Override
    public void loadProperty( PropertyLoader loader )
    {
        super.loadProperty( loader );
        
        if ( loader.loadProperty( fontColor2 ) );
        else if ( !loader.loadProperty(visibleTime) );
        
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
    
    public QualifInfosWidget()
    {
        super( PrunnWidgetSet.INSTANCE, PrunnWidgetSet.WIDGET_PACKAGE_WSBR, 50.0f, 20.5f );
        
        getBackgroundProperty().setColorValue( "#00000000" );
        getFontProperty().setFont( PrunnWidgetSet.WSBR_FONT_NAME );
        getFontColorProperty().setColor( PrunnWidgetSet.FONT_COLOR1_NAME );
    }
}
