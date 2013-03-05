package com.prunn.rfdynhud.widgets.prunn.wsbr.tracktemps;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;

import net.ctdp.rfdynhud.gamedata.LiveGameData;
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
import net.ctdp.rfdynhud.valuemanagers.Clock;
import net.ctdp.rfdynhud.values.IntValue;
import net.ctdp.rfdynhud.widgets.base.widget.Widget;
import com.prunn.rfdynhud.widgets.prunn._util.PrunnWidgetSet;

/**
 * 
 * 
 * @author Prunn 2011
 */


public class TrackTempsWidget extends Widget
{
    private final ImagePropertyWithTexture imgName = new ImagePropertyWithTexture( "imgName", "prunn/wsbr/wsbrname.png" );
    private final ImagePropertyWithTexture imgTime = new ImagePropertyWithTexture( "imgTime", "prunn/wsbr/wsbrlaptime.png" );
    
    private final ColorProperty fontColor2 = new ColorProperty( "fontColor2", PrunnWidgetSet.FONT_COLOR2_NAME );
    
    private DrawnString dsAmbient = null;
    private DrawnString dsAmbientTemp = null;
    private DrawnString dsTrack = null;
    private DrawnString dsTrackTemp = null;
    
    private final IntValue ambientTemp = new IntValue();
    private final IntValue trackTemp = new IntValue();
    
    
    
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
        int rowHeight = height / 2;
        
        int fieldWidth1 = Math.round( width * 0.68f );
        int fieldWidth2 = width - fieldWidth1;
        
        imgName.updateSize( fieldWidth1, rowHeight, isEditorMode );
        imgTime.updateSize( width - imgName.getTexture().getWidth(), rowHeight, isEditorMode );
        
        Color blackFontColor = getFontColor();
        Color whiteFontColor = fontColor2.getColor();
        
        dsAmbient = drawnStringFactory.newDrawnString( "dsAmbient", 10, rowHeight * 0 + ( rowHeight - fh ) / 2, Alignment.LEFT, false, getFont(), isFontAntiAliased(), blackFontColor );
        dsAmbientTemp = drawnStringFactory.newDrawnString( "dsAmbientTemp", fieldWidth1 + fieldWidth2 * 9 / 12, rowHeight * 0 + ( rowHeight - fh ) / 2, Alignment.RIGHT, false, getFont(), isFontAntiAliased(), whiteFontColor, null, "°C" );
        
        dsTrack = drawnStringFactory.newDrawnString( "dsTrack", 10, rowHeight * 1 + ( rowHeight - fh ) / 2, Alignment.LEFT, false, getFont(), isFontAntiAliased(), blackFontColor );
        dsTrackTemp = drawnStringFactory.newDrawnString( "dsTrackTemp", fieldWidth1 + fieldWidth2 * 9 / 12, rowHeight * 1 + ( rowHeight - fh ) / 2, Alignment.RIGHT, false, getFont(), isFontAntiAliased(), whiteFontColor, null, "°C" );
        
    }
    
    @Override
    protected void drawBackground( LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height, boolean isRoot )
    {
        super.drawBackground( gameData, isEditorMode, texture, offsetX, offsetY, width, height, isRoot );
        
        texture.clear( imgName.getTexture(), offsetX, offsetY, false, null );
        texture.clear( imgTime.getTexture(), offsetX + imgName.getTexture().getWidth(), offsetY, false, null );
        
        texture.clear( imgName.getTexture(), offsetX, offsetY + height/2, false, null );
        texture.clear( imgTime.getTexture(), offsetX + imgName.getTexture().getWidth(), offsetY + height/2, false, null );
    }
    
    
    @Override
    protected void drawWidget( Clock clock, boolean needsCompleteRedraw, LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height )
    {
        ambientTemp.update((int)Math.floor(gameData.getScoringInfo().getAmbientTemperature()));
        trackTemp.update((int)Math.floor(gameData.getScoringInfo().getTrackTemperature()));
        
        if ( needsCompleteRedraw || ambientTemp.hasChanged())
        {
            dsAmbient.draw( offsetX, offsetY, "Air Temperature", texture );
            dsAmbientTemp.draw( offsetX, offsetY, ambientTemp.getValueAsString(), texture);
        }
        if ( needsCompleteRedraw || trackTemp.hasChanged())
        {
            dsTrack.draw( offsetX, offsetY, "Track Temperature" , texture );
            dsTrackTemp.draw( offsetX, offsetY, trackTemp.getValueAsString(), texture);
        }
    }
    
    @Override
    public void saveProperties( PropertyWriter writer ) throws IOException
    {
        super.saveProperties( writer );
        
        writer.writeProperty( fontColor2, "" );
    }
    
    @Override
    public void loadProperty( PropertyLoader loader )
    {
        super.loadProperty( loader );
        
        if ( loader.loadProperty( fontColor2 ) );
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
    
    public TrackTempsWidget()
    {
        super( PrunnWidgetSet.INSTANCE, PrunnWidgetSet.WIDGET_PACKAGE_WSBR, 28.0f, 10.0f );
        
        getBackgroundProperty().setColorValue( "#00000000" );
        getFontProperty().setFont( PrunnWidgetSet.WSBR_FONT_NAME );
        getFontColorProperty().setColor( PrunnWidgetSet.FONT_COLOR1_NAME );
    }
}
