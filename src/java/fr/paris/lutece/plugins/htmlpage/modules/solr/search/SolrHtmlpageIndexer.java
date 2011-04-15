/*
 * Copyright (c) 2002-2008, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.htmlpage.modules.solr.search;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.demo.html.HTMLParser;
import fr.paris.lutece.plugins.htmlpage.business.HtmlPage;
import fr.paris.lutece.plugins.htmlpage.business.HtmlPageHome;
import fr.paris.lutece.plugins.htmlpage.service.HtmlPagePlugin;
import fr.paris.lutece.plugins.search.solr.business.field.Field;
import fr.paris.lutece.plugins.search.solr.indexer.SolrIndexer;
import fr.paris.lutece.plugins.search.solr.indexer.SolrItem;
import fr.paris.lutece.portal.service.content.XPageAppService;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.plugin.PluginService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.url.UrlItem;


/**
 * The Htmlpage indexer for Solr search platform
 *
 */
public class SolrHtmlpageIndexer implements SolrIndexer
{
    public static final String SHORT_NAME = "hpg";
    private static final String PROPERTY_DESCRIPTION = "htmlpage-solr.indexer.description";
    private static final String PROPERTY_NAME = "htmlpage-solr.indexer.name";
    private static final String PROPERTY_VERSION = "htmlpage-solr.indexer.version";
    private static final String PROPERTY_INDEXER_ENABLE = "htmlpage-solr.indexer.enable";
    private static final String SITE = AppPropertiesService.getProperty( "lutece.name" );
    private static final String PARAMETER_HTMLPAGE_ID = "htmlpage_id";

    public String getDescription(  )
    {
        return AppPropertiesService.getProperty( PROPERTY_DESCRIPTION );
    }

    public String getName(  )
    {
        return AppPropertiesService.getProperty( PROPERTY_NAME );
    }

    public String getVersion(  )
    {
        return AppPropertiesService.getProperty( PROPERTY_VERSION );
    }

    public Map<String, SolrItem> index(  )
    {
        Map<String, SolrItem> items = new HashMap<String, SolrItem>(  );

        String strPortalUrl = AppPathService.getPortalUrl(  );
        Plugin plugin = PluginService.getPlugin( HtmlPagePlugin.PLUGIN_NAME );

        Collection<HtmlPage> listHtmlPages = HtmlPageHome.findEnabledHtmlPageList( plugin );

        for ( HtmlPage htmlpage : listHtmlPages )
        {
            UrlItem url = new UrlItem( strPortalUrl );
            url.addParameter( XPageAppService.PARAM_XPAGE_APP, HtmlPagePlugin.PLUGIN_NAME );
            url.addParameter( PARAMETER_HTMLPAGE_ID, htmlpage.getId(  ) );

            SolrItem docHtmlPage;

            try
            {
                docHtmlPage = getDocument( htmlpage, url.getUrl(  ), plugin );
                items.put( getLog( docHtmlPage ), docHtmlPage );
            }
            catch ( IOException e )
            {
                AppLogService.error( e );
            }
        }

        return items;
    }

    public boolean isEnable(  )
    {
        return "true".equalsIgnoreCase( AppPropertiesService.getProperty( PROPERTY_INDEXER_ENABLE ) );
    }

    public List<Field> getAdditionalFields(  )
    {
        return null;
    }

    /**
     * Return a list of lucene document for incremental indexing
     * @param strId the uid of the document
     * @return listDocuments the document list
     */

    /*
    public List<Document> getDocuments( String strId )
        throws IOException, InterruptedException, SiteMessageException
    {
        ArrayList<org.apache.lucene.document.Document> listDocuments = new ArrayList<Document>(  );
        String strPortalUrl = AppPathService.getPortalUrl(  );
        Plugin plugin = PluginService.getPlugin( HtmlPagePlugin.PLUGIN_NAME );
    
        HtmlPage htmlpage = HtmlPageHome.findEnabledHtmlPage( Integer.parseInt( strId ), plugin );
        if( htmlpage != null )
        {
            UrlItem url = new UrlItem( strPortalUrl );
            url.addParameter( XPageAppService.PARAM_XPAGE_APP, HtmlPagePlugin.PLUGIN_NAME );
            url.addParameter( PARAMETER_HTMLPAGE_ID, htmlpage.getId(  ) );
    
            org.apache.lucene.document.Document docHtmlPage = getDocument( htmlpage, url.getUrl(  ), plugin );
    
            listDocuments.add( docHtmlPage );
        }
        return listDocuments;
    }
    */

    /**
     * Builds a solrItem which will be used by Solr during the indexing of the pages of the site with the following
     * fields : summary, uid, url, contents, title and description.
     * @return the built item
     * @param strUrl The base URL for documents
     * @param htmlpage the page to index
     * @param plugin The {@link Plugin}
     * @throws IOException The IO Exception
     */
    private SolrItem getDocument( HtmlPage htmlpage, String strUrl, Plugin plugin )
        throws IOException
    {
        // make a new, empty document
        SolrItem item = new SolrItem(  );

        // Setting the Url field
        item.setUrl( strUrl );

        // Setting the Uid field
        String strIdHtmlPage = String.valueOf( htmlpage.getId(  ) );
        item.setUid( strIdHtmlPage + "_" + SHORT_NAME );

        // Setting the Content field
        String strContentToIndex = getContentToIndex( htmlpage );
        StringReader readerPage = new StringReader( strContentToIndex );
        HTMLParser parser = new HTMLParser( readerPage );

        //the content of the question/answer is recovered in the parser because this one
        //had replaced the encoded caracters (as &eacute;) by the corresponding special caracter (as ?)
        Reader reader = parser.getReader(  );
        int c;
        StringBuffer sb = new StringBuffer(  );

        while ( ( c = reader.read(  ) ) != -1 )
        {
            sb.append( String.valueOf( (char) c ) );
        }

        reader.close(  );

        item.setContent( sb.toString(  ) );

        // Setting the Title field
        item.setTitle( htmlpage.getDescription(  ) );

        // Setting the Type field
        item.setType( HtmlPagePlugin.PLUGIN_NAME );
        
        // Setting the Site field
        item.setSite( SITE );


        // return the item
        return item;
    }

    /**
     * Set the Content to index
     * @param htmlpage The htmlpage to index
     * @return The content to index
     */
    private static String getContentToIndex( HtmlPage htmlpage )
    {
        StringBuffer sbContentToIndex = new StringBuffer(  );

        //index the title
        sbContentToIndex.append( htmlpage.getDescription(  ) );

        sbContentToIndex.append( " " );

        sbContentToIndex.append( htmlpage.getHtmlContent(  ) );

        return sbContentToIndex.toString(  );
    }

    /**
     * Generate the log line for the specified {@link SolrItem}
     * @param item The {@link SolrItem}
     * @return The string representing the log line
     */
    private String getLog( SolrItem item )
    {
        StringBuilder sbLogs = new StringBuilder(  );
        sbLogs.append( "indexing " );
        sbLogs.append( item.getType(  ) );
        sbLogs.append( " id : " );
        sbLogs.append( item.getUid(  ) );
        sbLogs.append( " Title : " );
        sbLogs.append( item.getTitle(  ) );
        sbLogs.append( "<br/>" );

        return sbLogs.toString(  );
    }
}