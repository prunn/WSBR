package com.prunn.rfdynhud.widgets.prunn.wsbr.qualtime;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;

import net.ctdp.rfdynhud.gamedata.LiveGameData;
import net.ctdp.rfdynhud.gamedata.ScoringInfo;
import net.ctdp.rfdynhud.gamedata.VehicleScoringInfo;
import net.ctdp.rfdynhud.properties.ColorProperty;
import net.ctdp.rfdynhud.properties.FontProperty;
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
import net.ctdp.rfdynhud.widgets.base.widget.Widget;
import com.prunn.rfdynhud.widgets.prunn._util.PrunnWidgetSet;

/**
 * 
 * 
 * @author Prunn 2011
 */


public class QualTimeWidget extends Widget
{
    private static enum Situation
    {
        LAST_SECONDS_OF_SECTOR_1,
        SECTOR_1_FINISHED_BEGIN_SECTOR_2,
        LAST_SECONDS_OF_SECTOR_2,
        SECTOR_2_FINISHED_BEGIN_SECTOR_3,
        LAST_SECONDS_OF_SECTOR_LAP,
        LAP_FINISHED_BEGIN_NEW_LAP,
        OTHER,
        ;
    }
    
    private static final float SECTOR_DELAY = 5f;
    
    private DrawnString dsPos = null;
    private DrawnString dsPosFrom = null;
    private DrawnString dsName = null;
    private DrawnString dsTime = null;
    private DrawnString dsGap = null;
    
    private final ImagePropertyWithTexture imgPos = new ImagePropertyWithTexture( "imgPos", "prunn/wsbr/wsbrpos.png" );
    private final ImagePropertyWithTexture imgPosFrom = new ImagePropertyWithTexture( "imgPosFrom", "prunn/wsbr/wsbrpos.png" );
    private final ImagePropertyWithTexture imgName = new ImagePropertyWithTexture( "imgName", "prunn/wsbr/wsbrname.png" );
    private final ImagePropertyWithTexture imgTime = new ImagePropertyWithTexture( "imgTime", "prunn/wsbr/wsbrlaptime.png" );
    private final ImagePropertyWithTexture imgTimeBlack = new ImagePropertyWithTexture( "imgTimeBlack", "prunn/wsbr/wsbrlaptime.png" );
    private final ImagePropertyWithTexture imgTimeGreen = new ImagePropertyWithTexture( "imgTimeGreen", "prunn/wsbr/wsbrgapgreen.png" );
    private final ImagePropertyWithTexture imgTimeYellow = new ImagePropertyWithTexture( "imgTimeYellow", "prunn/wsbr/wsbrgapyellow.png" );
    
    private final ColorProperty fontColor2 = new ColorProperty( "fontColor2", PrunnWidgetSet.FONT_COLOR2_NAME );
    private final FontProperty posFont = new FontProperty( "positionFont", PrunnWidgetSet.POS_FONT_NAME );
    private final ColorProperty gapFontColor1 = new ColorProperty( "gapColor1", PrunnWidgetSet.GAP_FONT_COLOR1_NAME );
    private final ColorProperty gapFontColor2 = new ColorProperty( "gapColor2", PrunnWidgetSet.GAP_FONT_COLOR2_NAME );
    
    private final EnumValue<Situation> situation = new EnumValue<Situation>();
    private final IntValue leaderID = new IntValue();
    private final IntValue leaderPos = new IntValue();
    private final IntValue ownPos = new IntValue();
    private float leadsec1 = -1f;
    private float leadsec2 = -1f;
    private float leadlap = -1f;
    private final FloatValue cursec1 = new FloatValue(-1f, 0.001f);
    private final FloatValue cursec2 = new FloatValue(-1f, 0.001f);
    private final FloatValue curlap = new FloatValue(-1f, 0.001f);
    private final FloatValue oldbesttime = new FloatValue(-1f, 0.001f);
    private final FloatValue gapOrTime = new FloatValue(-1f, 0.001f);
    private final FloatValue lastLaptime = new FloatValue(-1f, 0.001f);
    private final BoolValue gapAndTimeInvalid = new BoolValue();
    private float oldbest = 0;
    private long lapSystemTime = 0;
    private int gapRightOffset = 0;
    
    
    @Override
    public void onRealtimeEntered( LiveGameData gameData, boolean isEditorMode )
    {
        super.onRealtimeEntered( gameData, isEditorMode );
        
        situation.reset();
        leaderID.reset();
        leaderPos.reset();
        ownPos.reset();
        cursec1.reset();
        cursec2.reset();
        curlap.reset();
        oldbesttime.reset();
        gapOrTime.reset();
        lastLaptime.reset();
        gapAndTimeInvalid.reset();
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
        gapRightOffset = TextureImage2D.getStringWidth( "  ", getFontProperty() );
        
        int rowHeight = height / 3;
        int fh = TextureImage2D.getStringHeight( "09gy", getFontProperty() );
        int posfh = TextureImage2D.getStringHeight( "0", posFont );
        
        imgPos.updateSize( rowHeight *2, rowHeight *2, isEditorMode );
        imgName.updateSize( width - imgPos.getTexture().getWidth(), rowHeight, isEditorMode );
        imgTime.updateSize( width - imgPos.getTexture().getWidth(), rowHeight, isEditorMode );
        imgPosFrom.updateSize( rowHeight, rowHeight, isEditorMode );
        imgTimeBlack.updateSize( width - imgPos.getTexture().getWidth() - imgPosFrom.getTexture().getWidth(), rowHeight, isEditorMode );
        imgTimeGreen.updateSize( width - imgPos.getTexture().getWidth() - imgPosFrom.getTexture().getWidth(), rowHeight, isEditorMode );
        imgTimeYellow.updateSize( width - imgPos.getTexture().getWidth() - imgPosFrom.getTexture().getWidth(), rowHeight, isEditorMode );
        
        Color blackFontColor = getFontColor();
        Color whiteFontColor = fontColor2.getColor();
        
        int textOff = ( rowHeight - fh ) / 2;
        
        dsName = drawnStringFactory.newDrawnString( "dsName", imgName.getTexture().getWidth()-20, textOff, Alignment.RIGHT, false, getFont(), isFontAntiAliased(), blackFontColor);
        dsTime = drawnStringFactory.newDrawnString( "dsTime", imgName.getTexture().getWidth()-20, rowHeight + textOff, Alignment.RIGHT, false, getFont(), isFontAntiAliased(), whiteFontColor);
        dsPos = drawnStringFactory.newDrawnString( "dsPos", imgName.getTexture().getWidth() + imgPos.getTexture().getWidth()/2, imgPos.getTexture().getWidth()/2 - posfh/2, Alignment.CENTER, false, posFont.getFont(), isFontAntiAliased(), whiteFontColor);
        dsPosFrom = drawnStringFactory.newDrawnString( "dsPosFrom", rowHeight / 2, rowHeight * 2 + textOff, Alignment.CENTER, false, getFont(), isFontAntiAliased(), whiteFontColor );
        dsGap = drawnStringFactory.newDrawnString( "dsTime", imgName.getTexture().getWidth()-10, rowHeight * 2 + textOff, Alignment.RIGHT, false, getFont(), isFontAntiAliased(), blackFontColor);
        
    }
    
    private VehicleScoringInfo getLeaderCarInfos( ScoringInfo scoringInfo )
    {
        /*VehicleScoringInfo currentcarinfos = scoringInfo.getViewedVehicleScoringInfo();
        if(posKnockout.getValue() <= 2 || posKnockout.getValue() > scoringInfo.getNumVehicles() || currentcarinfos.getPlace( false )+3 < posKnockout.getValue() || scoringInfo.getVehicleScoringInfo( posKnockout.getValue()-1 ).getBestLapTime() < 0)
        {*/
            return ( scoringInfo.getLeadersVehicleScoringInfo() );
        /*}
        
        return ( scoringInfo.getVehicleScoringInfo( posKnockout.getValue()-1 ) );*/
    }
    
    private void updateSectorValues( ScoringInfo scoringInfo )
    {
        VehicleScoringInfo currentcarinfos = scoringInfo.getViewedVehicleScoringInfo();
        VehicleScoringInfo leadercarinfos = getLeaderCarInfos( scoringInfo );
        
        if(leadercarinfos.getFastestLaptime() != null && leadercarinfos.getFastestLaptime().getLapTime() >= 0)
        {
            leadsec1 = leadercarinfos.getFastestLaptime().getSector1();
            leadsec2 = leadercarinfos.getFastestLaptime().getSector1And2();
            leadlap = leadercarinfos.getFastestLaptime().getLapTime();
            
        }
        else
        {
            leadsec1 = 0f;
            leadsec2 = 0f;
            leadlap = 0f;
        }
        
        cursec1.update( currentcarinfos.getCurrentSector1() );
        cursec2.update( currentcarinfos.getCurrentSector2( true ) );
        
        if ( scoringInfo.getSessionTime() > 0f )
            curlap.update( currentcarinfos.getCurrentLaptime() );
        else
            curlap.update( (System.currentTimeMillis() - lapSystemTime + 300)/1000f );
    }
    
    private boolean updateSituation( VehicleScoringInfo currentcarinfos )
    {
        final byte sector = currentcarinfos.getSector();
        
        if(sector == 1 && curlap.getValue() > leadsec1 - SECTOR_DELAY && leadlap > 0)
        {
            situation.update( Situation.LAST_SECONDS_OF_SECTOR_1 );
        }
        else if(sector == 2 && curlap.getValue() - cursec1.getValue() <= SECTOR_DELAY && leadlap > 0)
        {
            situation.update( Situation.SECTOR_1_FINISHED_BEGIN_SECTOR_2 );
        }
        else if(sector == 2  && curlap.getValue() > leadsec2 - SECTOR_DELAY && leadlap > 0)
        {
            situation.update( Situation.LAST_SECONDS_OF_SECTOR_2 );
        }
        else if(sector == 3 && curlap.getValue() - cursec2.getValue() <= SECTOR_DELAY && leadlap > 0)
        {
            situation.update( Situation.SECTOR_2_FINISHED_BEGIN_SECTOR_3 );
        }
        else if(sector == 3 && curlap.getValue() > leadlap - SECTOR_DELAY && leadlap > 0)
        {
            situation.update( Situation.LAST_SECONDS_OF_SECTOR_LAP );
        }
        else if(sector == 1 && curlap.getValue() <= SECTOR_DELAY && currentcarinfos.getLastLapTime() > 0)
        {
            situation.update( Situation.LAP_FINISHED_BEGIN_NEW_LAP );
        }
        else
        {
            situation.update( Situation.OTHER );
        }
        
        return ( situation.hasChanged() );
    }
    
    @Override
    protected Boolean updateVisibility(LiveGameData gameData, boolean isEditorMode)
    {
        
        super.updateVisibility(gameData, isEditorMode);
        
        ScoringInfo scoringInfo = gameData.getScoringInfo();
        updateSectorValues( scoringInfo );
        VehicleScoringInfo currentcarinfos = scoringInfo.getViewedVehicleScoringInfo();
        
        if (scoringInfo.getPlayersVehicleScoringInfo().isLapJustStarted())
            lapSystemTime = System.currentTimeMillis();
        
        if ( updateSituation( currentcarinfos ) )
            forceCompleteRedraw( true );
        
        if ( currentcarinfos.isInPits() )
        {
            return false;
        }
        
        float curLaptime;
        if ( scoringInfo.getSessionTime() > 0f )
            curLaptime = currentcarinfos.getCurrentLaptime();
        else
            curLaptime = (System.currentTimeMillis() - lapSystemTime + 300)/1000f;
        
        if ( curLaptime > 0f )
        {
            return true;
        }
            
        return false;
         
    }
    
    @Override
    protected void drawBackground( LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height, boolean isRoot )
    {
        super.drawBackground( gameData, isEditorMode, texture, offsetX, offsetY, width, height, isRoot );
        
        VehicleScoringInfo currentcarinfos = gameData.getScoringInfo().getViewedVehicleScoringInfo();
        
        int rowHeight = height / 3;
        
        texture.clear( imgName.getTexture(), offsetX, offsetY, false, null );
        texture.clear( imgTime.getTexture(), offsetX, offsetY + rowHeight, false, null );
        
        switch ( situation.getValue() )
        {
            case LAST_SECONDS_OF_SECTOR_1:
                texture.clear( imgTimeBlack.getTexture(), offsetX + imgPosFrom.getTexture().getWidth(), offsetY + rowHeight*2, false, null );
                texture.clear( imgPosFrom.getTexture(), offsetX, offsetY + rowHeight*2, false, null );
                //texture.clear(offsetX + imgTime.getTexture().getWidth(), offsetY, imgPos.getTexture().getWidth(), imgPos.getTexture().getHeight(), true, null);
                break;
                
            case SECTOR_1_FINISHED_BEGIN_SECTOR_2:
                if( cursec1.getValue() <= leadsec1 )
                    texture.clear( imgTimeGreen.getTexture(), offsetX + imgPosFrom.getTexture().getWidth(), offsetY + rowHeight*2, false, null );
                else
                    texture.clear( imgTimeYellow.getTexture(), offsetX + imgPosFrom.getTexture().getWidth(), offsetY + rowHeight*2, false, null );
                 
                texture.clear( imgPosFrom.getTexture(), offsetX, offsetY + rowHeight*2, false, null );
                //texture.clear(offsetX + imgTime.getTexture().getWidth(), offsetY, imgPos.getTexture().getWidth(), imgPos.getTexture().getHeight(), true, null);
                break;
                
            case LAST_SECONDS_OF_SECTOR_2:
                texture.clear( imgTimeBlack.getTexture(), offsetX + imgPosFrom.getTexture().getWidth(), offsetY + rowHeight*2, false, null );
                texture.clear( imgPosFrom.getTexture(), offsetX, offsetY + rowHeight*2, false, null );
                //texture.clear(offsetX + imgTime.getTexture().getWidth(), offsetY, imgPos.getTexture().getWidth(), imgPos.getTexture().getHeight(), true, null);
                break;
                
            case SECTOR_2_FINISHED_BEGIN_SECTOR_3:
                if( cursec2.getValue() <= leadsec2 )
                    texture.clear( imgTimeGreen.getTexture(), offsetX + imgPosFrom.getTexture().getWidth(), offsetY + rowHeight*2, false, null );
                else
                    texture.clear( imgTimeYellow.getTexture(), offsetX + imgPosFrom.getTexture().getWidth(), offsetY + rowHeight*2, false, null );
                     
                texture.clear( imgPosFrom.getTexture(), offsetX, offsetY + rowHeight*2, false, null );
                //texture.clear(offsetX + imgTime.getTexture().getWidth(), offsetY, imgPos.getTexture().getWidth(), imgPos.getTexture().getHeight(), true, null);
                break;
                
            case LAST_SECONDS_OF_SECTOR_LAP:
                texture.clear( imgTimeBlack.getTexture(), offsetX + imgPosFrom.getTexture().getWidth(), offsetY + rowHeight*2, false, null );
                texture.clear( imgPosFrom.getTexture(), offsetX, offsetY + rowHeight*2, false, null );
                //texture.clear(offsetX + imgTime.getTexture().getWidth(), offsetY, imgPos.getTexture().getWidth(), imgPos.getTexture().getHeight(), true, null);
                break;
                
            case LAP_FINISHED_BEGIN_NEW_LAP:
                if( currentcarinfos.getLastLapTime() <= leadlap )
                    texture.clear( imgTimeGreen.getTexture(), offsetX + imgPosFrom.getTexture().getWidth(), offsetY + rowHeight*2, false, null );
                else
                    texture.clear( imgTimeYellow.getTexture(), offsetX + imgPosFrom.getTexture().getWidth(), offsetY + rowHeight*2, false, null );
                    
                texture.clear( imgPosFrom.getTexture(), offsetX, offsetY + rowHeight*2, false, null );
                texture.clear( imgPos.getTexture(), offsetX + imgTime.getTexture().getWidth(), offsetY, false, null );
                break;
                
            //case OTHER:
                // other cases not info not drawn
                //texture.clear(offsetX, offsetY + rowHeight*2, width, imgTimeBlack.getTexture().getHeight(), true, null);
                //texture.clear(offsetX + imgTime.getTexture().getWidth(), offsetY, imgPos.getTexture().getWidth(), imgPos.getTexture().getHeight(), true, null);
                //break;
        }
    }
    
    private static final String getTimeAsGapString2( float gap )
    {
        if ( gap == 0f )
            return ( "- " + TimingUtil.getTimeAsLaptimeString( 0f ) );
        
        if ( gap < 0f )
            return ( "- " + TimingUtil.getTimeAsLaptimeString( -gap ) );
        
        return ( "+ " + TimingUtil.getTimeAsLaptimeString( gap ) );
    }
    
    @Override
    protected void drawWidget( Clock clock, boolean needsCompleteRedraw, LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height )
    {
        ScoringInfo scoringInfo = gameData.getScoringInfo();
        updateSectorValues( scoringInfo );
        VehicleScoringInfo currentcarinfos = scoringInfo.getViewedVehicleScoringInfo();
        VehicleScoringInfo leadercarinfos = getLeaderCarInfos( scoringInfo );
        
        leaderID.update( leadercarinfos.getDriverId() );
        leaderPos.update( leadercarinfos.getPlace( false ) );
        
        if ( needsCompleteRedraw || ( clock.c() && leaderID.hasChanged() ) )
        {
            dsName.draw( offsetX, offsetY, currentcarinfos.getDriverNameShort(), texture );
        }
    	
        switch ( situation.getValue() )
        {
            case LAST_SECONDS_OF_SECTOR_1:
                gapAndTimeInvalid.update( false );
                gapOrTime.update( leadsec1 );
                
                if ( needsCompleteRedraw || ( clock.c() && gapOrTime.hasChanged() ) )
                    dsGap.draw( offsetX, offsetY, TimingUtil.getTimeAsLaptimeString( gapOrTime.getValue() ), gapFontColor1.getColor() , texture);
                if ( needsCompleteRedraw || ( clock.c() && leaderPos.hasChanged() ) )
                    dsPosFrom.draw( offsetX, offsetY, leaderPos.getValueAsString(), texture );  
                if ( needsCompleteRedraw || ( clock.c() && curlap.hasChanged() ) )
                    dsTime.draw( offsetX - gapRightOffset, offsetY, TimingUtil.getTimeAsString( curlap.getValue(), false, false, true, false ), texture);
                break;
                
            case SECTOR_1_FINISHED_BEGIN_SECTOR_2:
                gapAndTimeInvalid.update( false );
                gapOrTime.update( cursec1.getValue() - leadsec1 );
                
                if ( needsCompleteRedraw || ( clock.c() && gapOrTime.hasChanged() ) )
                    dsGap.draw( offsetX - gapRightOffset, offsetY, getTimeAsGapString2( gapOrTime.getValue() ), ( gapOrTime.getValue() <= 0 ) ? gapFontColor1.getColor() : gapFontColor2.getColor() , texture);
                if ( needsCompleteRedraw || ( clock.c() && leaderPos.hasChanged() ) )
                    dsPosFrom.draw( offsetX, offsetY, leaderPos.getValueAsString(), texture );
                if ( needsCompleteRedraw || ( clock.c() && cursec1.hasChanged() ) )
                    dsTime.draw( offsetX, offsetY, TimingUtil.getTimeAsString( cursec1.getValue(), false, false, true, true ) , texture);
                break;
                
            case LAST_SECONDS_OF_SECTOR_2:
                gapAndTimeInvalid.update( false );
                gapOrTime.update( leadsec2 );
                
                if ( needsCompleteRedraw || ( clock.c() && gapOrTime.hasChanged() ) )
                    dsGap.draw( offsetX, offsetY, TimingUtil.getTimeAsLaptimeString( leadsec2 ), gapFontColor1.getColor() , texture);
                if ( needsCompleteRedraw || ( clock.c() && leaderPos.hasChanged() ) )
                    dsPosFrom.draw( offsetX, offsetY, leaderPos.getValueAsString(), texture );
                if ( needsCompleteRedraw || ( clock.c() && curlap.hasChanged() ) )
                    dsTime.draw( offsetX - gapRightOffset, offsetY, TimingUtil.getTimeAsString( curlap.getValue(), false, false, true, false ), texture);
                break;
                
            case SECTOR_2_FINISHED_BEGIN_SECTOR_3:
                gapAndTimeInvalid.update( false );
                gapOrTime.update( cursec2.getValue() - leadsec2 );
                
                if ( needsCompleteRedraw || ( clock.c() && gapOrTime.hasChanged() ) )
                    dsGap.draw( offsetX - gapRightOffset, offsetY, getTimeAsGapString2( gapOrTime.getValue() ), ( gapOrTime.getValue() <= 0 ) ? gapFontColor1.getColor() : gapFontColor2.getColor() , texture);
                if ( needsCompleteRedraw || ( clock.c() && leaderPos.hasChanged() ) )
                    dsPosFrom.draw( offsetX, offsetY, leaderPos.getValueAsString(), texture );
                if ( needsCompleteRedraw || ( clock.c() && cursec2.hasChanged() ) )
                    dsTime.draw( offsetX, offsetY, TimingUtil.getTimeAsString( cursec2.getValue(), false, false, true, true ) , texture);
                break;
                
            case LAST_SECONDS_OF_SECTOR_LAP:
                gapAndTimeInvalid.update( false );
                gapOrTime.update( leadlap );
                
                if ( needsCompleteRedraw || ( clock.c() && gapOrTime.hasChanged() ) )
                    dsGap.draw( offsetX, offsetY, TimingUtil.getTimeAsLaptimeString( gapOrTime.getValue() ), gapFontColor1.getColor() , texture);
                if ( needsCompleteRedraw || ( clock.c() && leaderPos.hasChanged() ) )
                    dsPosFrom.draw( offsetX, offsetY, leaderPos.getValueAsString(), texture );
                if ( needsCompleteRedraw || ( clock.c() && curlap.hasChanged() ) )
                    dsTime.draw( offsetX - gapRightOffset, offsetY, TimingUtil.getTimeAsString( curlap.getValue(), false, false, true, false ), texture);
                break;
                
            case LAP_FINISHED_BEGIN_NEW_LAP:
                
                //plan: if allready first show gap to previous own best time. doesnt work at the moment; if newly first show gap to second                             
                
                float secondbest;
                oldbesttime.update( currentcarinfos.getBestLapTime() );
                
                if(oldbesttime.hasChanged())
                    oldbest = oldbesttime.getOldValue();
            
                if(gameData.getScoringInfo().getSecondFastestLapVSI() != null)
                    secondbest = gameData.getScoringInfo().getSecondFastestLapVSI().getBestLapTime(); 
                else
                    secondbest = oldbest;
                
                if (currentcarinfos.getLastLapTime() <= leadercarinfos.getBestLapTime() && secondbest < 0)
                    gapOrTime.update(  currentcarinfos.getLastLapTime() - oldbest );
                else
                    if( currentcarinfos.getLastLapTime() <= leadercarinfos.getBestLapTime() )
                        gapOrTime.update( currentcarinfos.getLastLapTime() - secondbest );
                    else
                        gapOrTime.update( currentcarinfos.getLastLapTime() - leadercarinfos.getBestLapTime() );
                    
                if ( needsCompleteRedraw || ( clock.c() && gapOrTime.hasChanged() ) )
                    dsGap.draw( offsetX - gapRightOffset, offsetY, getTimeAsGapString2( gapOrTime.getValue() ), ( gapOrTime.getValue() <= 0 ) ? gapFontColor1.getColor() : gapFontColor2.getColor(), texture);
                
                
                
                ownPos.update( currentcarinfos.getPlace( false ) );
                lastLaptime.update( currentcarinfos.getLastLapTime() );
                gapAndTimeInvalid.update( false );
                
                if ( needsCompleteRedraw || ( clock.c() && leaderPos.hasChanged() ) )
                    dsPosFrom.draw( offsetX, offsetY, leaderPos.getValueAsString(), texture );
                if ( needsCompleteRedraw || ( clock.c() && ownPos.hasChanged() ) )
                    dsPos.draw( offsetX, offsetY, ownPos.getValueAsString(), texture );
                if ( needsCompleteRedraw || ( clock.c() && lastLaptime.hasChanged() ) )
                    dsTime.draw( offsetX, offsetY, TimingUtil.getTimeAsString( lastLaptime.getValue(), false, false, true, true ) , texture);
                break;
              
            case OTHER:
                // other cases not info not drawn
                gapAndTimeInvalid.update( true );
                
                if ( needsCompleteRedraw || ( clock.c() && gapAndTimeInvalid.hasChanged() ) )
                {
                    dsGap.draw( offsetX, offsetY, "", texture);
                    dsPosFrom.draw( offsetX, offsetY, "", texture );
                }
                if ( needsCompleteRedraw || ( clock.c() && curlap.hasChanged() ) )
                    dsTime.draw( offsetX - gapRightOffset, offsetY, TimingUtil.getTimeAsString( curlap.getValue(), false, false, true, false ), texture);
                break;
        }
    }
    
    
    @Override
    public void saveProperties( PropertyWriter writer ) throws IOException
    {
        super.saveProperties( writer );
        
        writer.writeProperty( fontColor2, "" );
        writer.writeProperty( posFont, "" );
        writer.writeProperty( gapFontColor1, "" );
        writer.writeProperty( gapFontColor2, "" );
        //writer.writeProperty( posKnockout, "" );
    }
    
    @Override
    public void loadProperty( PropertyLoader loader )
    {
        super.loadProperty( loader );
        
        if ( loader.loadProperty( fontColor2 ) );
        else if ( loader.loadProperty( posFont ) );
        else if ( loader.loadProperty( gapFontColor1 ) );
        else if ( loader.loadProperty( gapFontColor2 ) );
        //else if ( loader.loadProperty( posKnockout ) );
    }
    
    @Override
    protected void addFontPropertiesToContainer( PropertiesContainer propsCont, boolean forceAll )
    {
        propsCont.addGroup( "Colors and Fonts" );
        
        super.addFontPropertiesToContainer( propsCont, forceAll );
        
        propsCont.addProperty( fontColor2 );
        propsCont.addProperty( posFont );
        propsCont.addProperty( gapFontColor1 );
        propsCont.addProperty( gapFontColor2 );
    }
    
    @Override
    public void getProperties( PropertiesContainer propsCont, boolean forceAll )
    {
        super.getProperties( propsCont, forceAll );
        
        propsCont.addGroup( "Specific" );
        
        //propsCont.addProperty( posKnockout );
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
    
    public QualTimeWidget()
    {
        super( PrunnWidgetSet.INSTANCE, PrunnWidgetSet.WIDGET_PACKAGE_WSBR, 30.0f, 17.1f );
        
        getBackgroundProperty().setColorValue( "#00000000" );
        getFontProperty().setFont( PrunnWidgetSet.WSBR_FONT_NAME );
        getFontColorProperty().setColor( PrunnWidgetSet.FONT_COLOR1_NAME );
    }
}
