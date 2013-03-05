package com.prunn.rfdynhud.widgets.prunn.wsbr.racetimingtower;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;

import net.ctdp.rfdynhud.gamedata.GamePhase;
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
import net.ctdp.rfdynhud.util.PropertyWriter;
import net.ctdp.rfdynhud.util.SubTextureCollector;
import net.ctdp.rfdynhud.util.TimingUtil;
import net.ctdp.rfdynhud.valuemanagers.Clock;
import net.ctdp.rfdynhud.values.IntValue;
import net.ctdp.rfdynhud.widgets.base.widget.Widget;
import com.prunn.rfdynhud.widgets.prunn._util.PrunnWidgetSet;

/**
 * 
 * 
 * @author Prunn 2011
 */


public class RaceTimingTowerWidget extends Widget
{
    private final ImagePropertyWithTexture imgPos = new ImagePropertyWithTexture( "imgPos", "prunn/wsbr/wsbrpos.png" );
    private final ImagePropertyWithTexture imgName = new ImagePropertyWithTexture( "imgName", "prunn/wsbr/wsbrname.png" );
    private final ImagePropertyWithTexture imgTime = new ImagePropertyWithTexture( "imgTime", "prunn/wsbr/wsbrlaptime.png" );
    private final ImagePropertyWithTexture imgPositive = new ImagePropertyWithTexture( "imgTime", "prunn/wsbr/wsbrpospositive.png" );
    private final ImagePropertyWithTexture imgNegative = new ImagePropertyWithTexture( "imgTime", "prunn/wsbr/wsbrposnegative.png" );
    private final ImagePropertyWithTexture imgNeutral = new ImagePropertyWithTexture( "imgTime", "prunn/wsbr/wsbrposneutral.png" );
    private final ColorProperty fontColor2 = new ColorProperty( "fontColor2", PrunnWidgetSet.FONT_COLOR2_NAME );
    private final BooleanProperty showPitstops = new BooleanProperty( "showPitstops", false);
    private final DelayProperty visibleTime = new DelayProperty( "visibleTime", DelayProperty.DisplayUnits.SECONDS, 12 );
    private long visibleEnd = 0;
    private DrawnString[] dsPos = null;
    private DrawnString[] dsName = null;
    private DrawnString[] dsTime = null;
    private final IntProperty numVeh = new IntProperty( "numberOfVehicles", 8 );
    private int[] startedPositions = null;
    private boolean startedPositionsInitialized = false;
    private final IntValue currentLap = new IntValue();
    private short shownData = 0; //0-2-4-gaps 1-place gained 5-pitstop
    private final IntValue drawnCars = new IntValue();
    private final IntValue carsOnLeadLap = new IntValue();
    
    private short[] positions = null;
    private short[] gainedPlaces = null;
    private String[] names = null;
    private String[] gaps = null;
    
    
    
    @Override
    public void onRealtimeEntered( LiveGameData gameData, boolean isEditorMode )
    {
        super.onRealtimeEntered( gameData, isEditorMode );
        
        drawnCars.reset();
        visibleEnd = 0;
        
        
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
        int maxNumItems = numVeh.getValue();
        dsPos = new DrawnString[maxNumItems];
        dsName = new DrawnString[maxNumItems];
        dsTime = new DrawnString[maxNumItems];
        
        int fh = TextureImage2D.getStringHeight( "0%C", getFontProperty() );
        int rowHeight = height / maxNumItems;
        int fw2 = Math.round(width * 0.32f);
        int fw3 = width - rowHeight - fw2;
        int fw3b = fw3 * 3 / 4;

        imgPos.updateSize( rowHeight, rowHeight, isEditorMode );
        imgName.updateSize( fw2, rowHeight, isEditorMode );
        imgTime.updateSize( fw3, rowHeight, isEditorMode );
        imgPositive.updateSize( fw3b, rowHeight, isEditorMode );
        imgNegative.updateSize( fw3b, rowHeight, isEditorMode );
        imgNeutral.updateSize( fw3b, rowHeight, isEditorMode );
        
        Color blackFontColor = getFontColor();
        Color whiteFontColor = fontColor2.getColor();
        
        int top = ( rowHeight - fh ) / 2;
        
        for(int i=0;i < maxNumItems;i++)
        { 
            dsPos[i] = drawnStringFactory.newDrawnString( "dsPos", imgPos.getTexture().getWidth()/2, top, Alignment.CENTER, false, getFont(), isFontAntiAliased(), whiteFontColor );
            dsName[i] = drawnStringFactory.newDrawnString( "dsName", imgPos.getTexture().getWidth()+10, top, Alignment.LEFT, false, getFont(), isFontAntiAliased(), blackFontColor );
            dsTime[i] = drawnStringFactory.newDrawnString( "dsTime", imgPos.getTexture().getWidth() + imgName.getTexture().getWidth() + imgTime.getTexture().getWidth()*5/6, top, Alignment.RIGHT, false, getFont(), isFontAntiAliased(), whiteFontColor );
            
            top += rowHeight;
        }
    }
    
    private void clearArrayValues(int maxNumCars)
    {
        positions = new short[maxNumCars];
        gainedPlaces = new short[maxNumCars];
        gaps = new String[maxNumCars];
        names = new String[maxNumCars];
        
        for(int i=0;i<maxNumCars;i++)
        {
            positions[i] = -1;
            gainedPlaces[i] = 0;
            gaps[i] = "";
            names[i] = "";
        }
    }
    private void FillArrayValues(int onLeaderLap, ScoringInfo scoringInfo, int data, boolean isEditorMode)
    {
        if(isEditorMode)
            onLeaderLap = numVeh.getValue();
        
        for(int i=0;i < onLeaderLap;i++)
        {
            
            if(positions[i] == -1)
            {
                
            
                VehicleScoringInfo vsi = scoringInfo.getVehicleScoringInfo( i );
                positions[i] = vsi.getPlace( false );
                names[i] = vsi.getDriverNameTLC( true );
                
                switch(data) //0-2-4-gaps 1-place gained 5-pitstop
                {
                    case 1: //places
                            int startedfrom=0;
                            for(int p=0; p < scoringInfo.getNumVehicles(); p++)
                            {
                                if( vsi.getDriverId() == startedPositions[p] )
                                {
                                    startedfrom = p+1;
                                    break;
                                } 
                            }
                            gainedPlaces[i] = (short)( startedfrom - vsi.getPlace( false ) );
                            gaps[i] = String.valueOf( Math.abs( startedfrom - vsi.getPlace( false )) ) + "    ";
                            break;
                    case 5: //pitstops
                            gaps[i] = vsi.getNumPitstopsMade() + " STOP";
                            break;
                    default: //gaps
                            if(i==0)
                                gaps[i] = "LAP " + vsi.getLapsCompleted();
                            else
                                gaps[i] = "+ " + TimingUtil.getTimeAsLaptimeString( Math.abs(vsi.getTimeBehindLeader( false )) );
                            
                            break;
                }
            }
        }
    }
    private void initStartedFromPositions( ScoringInfo scoringInfo )
    {
        startedPositions = new int[scoringInfo.getNumVehicles()];
        
        for(int j=0;j < scoringInfo.getNumVehicles(); j++)
            startedPositions[j] = scoringInfo.getVehicleScoringInfo( j ).getDriverId();
        
        startedPositionsInitialized = true;
    }
    
    @Override
    protected Boolean updateVisibility( LiveGameData gameData, boolean isEditorMode )
    {
        super.updateVisibility( gameData, isEditorMode );
        
        ScoringInfo scoringInfo = gameData.getScoringInfo();
        
        if ( !startedPositionsInitialized )
            initStartedFromPositions( scoringInfo );
        
        currentLap.update( scoringInfo.getLeadersVehicleScoringInfo().getLapsCompleted() );
        
        if( currentLap.hasChanged() && currentLap.getValue() > 0 || isEditorMode )
        {
            
            //fetch what data is shown others-gaps 1-places gained/lost 3-pitstop made
            if(scoringInfo.getLeadersVehicleScoringInfo().getFinishStatus().isFinished() || isEditorMode)
                shownData = 0 ;
            else
                shownData = (short)( Math.random() * (showPitstops.getValue() ? 5 : 2) );
            
            if(gameData.getScoringInfo().getGamePhase() != GamePhase.SESSION_OVER)
                visibleEnd = scoringInfo.getSessionNanos() + visibleTime.getDelayNanos();
            else
                visibleEnd = scoringInfo.getSessionNanos() + visibleTime.getDelayNanos()*2;
            
            clearArrayValues(scoringInfo.getNumVehicles());
            FillArrayValues( 1, scoringInfo, shownData, isEditorMode);
            if(!isEditorMode)
                forceCompleteRedraw( true );
            return true;
            
        }
        
        if(scoringInfo.getSessionNanos() < visibleEnd || isEditorMode)
        {
            //how many on the same lap?
            int onlap = 0;
            for(int j=0;j < scoringInfo.getNumVehicles(); j++)
            {
                if(scoringInfo.getVehicleScoringInfo( j ).getLapsCompleted() == scoringInfo.getLeadersVehicleScoringInfo().getLapsCompleted() )
                    onlap++;
            }
            carsOnLeadLap.update( onlap );
            if (carsOnLeadLap.hasChanged() && !isEditorMode )
            {
                FillArrayValues( onlap, scoringInfo, shownData, false);
                forceCompleteRedraw( true );
            }
            return true;
        }
        
        
        return false;
    }
    @Override
    protected void drawBackground( LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height, boolean isRoot )
    {
        super.drawBackground( gameData, isEditorMode, texture, offsetX, offsetY, width, height, isRoot );
        
        ScoringInfo scoringInfo = gameData.getScoringInfo();
        int maxNumItems = numVeh.getValue();
        int rowHeight = height / maxNumItems;
        int drawncars = Math.min( scoringInfo.getNumVehicles(), maxNumItems );
        short posOffset;
        
        for(int i=0;i < drawncars;i++)
        {
            if(positions[i] != -1 || isEditorMode)
            {
                texture.clear( imgPos.getTexture(), offsetX, offsetY+rowHeight*i, false, null );
                texture.clear( imgName.getTexture(), offsetX + imgPos.getTexture().getWidth(), offsetY+rowHeight*i, false, null );
                
                switch( shownData )
                {
                    case 1: 
                            if(carsOnLeadLap.getValue() > numVeh.getValue() && i != 0)
                                posOffset = (short)( carsOnLeadLap.getValue() - numVeh.getValue() );
                            else
                                posOffset = 0;
                            
                            if(gainedPlaces[i + posOffset] > 0)
                                texture.clear( imgPositive.getTexture(), offsetX + imgPos.getTexture().getWidth() + imgName.getTexture().getWidth(), offsetY+rowHeight*i, true, null ); 
                            else 
                                if(gainedPlaces[i + posOffset] < 0)
                                    texture.clear( imgNegative.getTexture(), offsetX + imgPos.getTexture().getWidth() + imgName.getTexture().getWidth(), offsetY+rowHeight*i, true, null ); 
                                else
                                    texture.clear( imgNeutral.getTexture(), offsetX + imgPos.getTexture().getWidth() + imgName.getTexture().getWidth(), offsetY+rowHeight*i, true, null ); 
                            break;
                        
                    default:
                            texture.clear( imgTime.getTexture(), offsetX + imgPos.getTexture().getWidth() + imgName.getTexture().getWidth(), offsetY+rowHeight*i, true, null );
                            break;
                }
            }
        }
    }
    
    
    @Override
    protected void drawWidget( Clock clock, boolean needsCompleteRedraw, LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height )
    {
        ScoringInfo scoringInfo = gameData.getScoringInfo();
        
        if ( needsCompleteRedraw )
        {
            int drawncars = Math.min( scoringInfo.getNumVehicles(), numVeh.getValue() );
            short posOffset;
            
            for(int i=0;i < drawncars;i++)
            { 
                if(carsOnLeadLap.getValue() > numVeh.getValue() && i != 0)
                    posOffset = (short)( carsOnLeadLap.getValue() - numVeh.getValue() );
                else
                    posOffset = 0;
                
                
                if(positions[i + posOffset] != -1)
                    dsPos[i].draw( offsetX, offsetY, String.valueOf(positions[i + posOffset]), texture );
                else
                    dsPos[i].draw( offsetX, offsetY, "", texture );
                
                dsName[i].draw( offsetX, offsetY, names[i + posOffset], texture );
                dsTime[i].draw( offsetX, offsetY, gaps[i + posOffset], texture );
            }
        }
    }
    
    
    @Override
    public void saveProperties( PropertyWriter writer ) throws IOException
    {
        super.saveProperties( writer );
        
        writer.writeProperty( fontColor2, "" );
        writer.writeProperty( numVeh, "" );
        writer.writeProperty( visibleTime, "" );
        writer.writeProperty( showPitstops, "" );
    }
    
    @Override
    public void loadProperty( PropertyLoader loader )
    {
        super.loadProperty( loader );
        
        if ( loader.loadProperty( fontColor2 ) );
        else if ( loader.loadProperty( numVeh ) );
        else if ( !loader.loadProperty( visibleTime ) );
        else if ( loader.loadProperty( showPitstops ) );
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
        
        propsCont.addProperty( numVeh );
        propsCont.addProperty( visibleTime );
        propsCont.addProperty( showPitstops );
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
    
    public RaceTimingTowerWidget()
    {
        super( PrunnWidgetSet.INSTANCE, PrunnWidgetSet.WIDGET_PACKAGE_WSBR, 20.0f, 32.5f );
        
        getBackgroundProperty().setColorValue( "#00000000" );
        getFontProperty().setFont( PrunnWidgetSet.WSBR_FONT_NAME );
        getFontColorProperty().setColor( PrunnWidgetSet.FONT_COLOR1_NAME );
    }
}
