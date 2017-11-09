/**
 * Projeto   : GeradorDeRelatorio
 * Pacote    : br.jus.trt10.relatorio.fabrica
 * Classe    : FabricaRelatorio.java
 * Autor     : wilson.souza
 * Data      : 30/09/2016
 * Descrição : 
 * 
 * Modificações 
 * 
 * Responsavel  Data        Descrição
 * ===========  ==========  =====================================================               
 *
 */

package br.com.wcs80.relatorio.fabrica;

import br.com.wcs80.relatorio.util.TipoPath;
import br.com.wcs80.relatorio.util.TipoRelatorio;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.faces.context.FacesContext;
import javax.naming.InitialContext;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import net.sf.jasperreports.crosstabs.JRCrosstab;
import net.sf.jasperreports.engine.JRBreak;
import net.sf.jasperreports.engine.JRChart;
import net.sf.jasperreports.engine.JRComponentElement;
import net.sf.jasperreports.engine.JRElementGroup;
import net.sf.jasperreports.engine.JREllipse;
import net.sf.jasperreports.engine.JRFrame;
import net.sf.jasperreports.engine.JRGenericElement;
import net.sf.jasperreports.engine.JRImage;
import net.sf.jasperreports.engine.JRLine;
import net.sf.jasperreports.engine.JRRectangle;
import net.sf.jasperreports.engine.JRStaticText;
import net.sf.jasperreports.engine.JRSubreport;
import net.sf.jasperreports.engine.JRTextField;
import net.sf.jasperreports.engine.JRVisitor;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.util.JRElementsVisitor;
import net.sf.jasperreports.engine.util.JRSaver;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import net.sf.jasperreports.export.Exporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.apache.commons.io.FileUtils;

public class FabricaRelatorio {

	private ArrayList<String> completedSubReport   = new ArrayList<String>();
	private Throwable         subReportExceptioon  = null;
	private Connection        conOracle            = null;
	private InitialContext    initCtx              = null;
	private File              fileZip              = null;
	private String            jndiNameDataSource   = null;
	
	//Diretório temporário para compilação e execução dos relatorios.
	private File tmpDir = null;
	
	FabricaRelatorio() {}
	
	public FabricaRelatorio(String pathZip, TipoPath tipoPath){
		
		switch (tipoPath) {
		case ABSOLUTO:
			carregarArquivoAbsoluto(pathZip);
			break;

		case RELATIVO:
			carregarArquivoRelativo(pathZip);
			break;
		}
		
	}
	
	
	/**
	 * Cria a instância da FabricaRelatorio informando o path do arquivo zip.
	 * @param pathZip - Ex: /WEB-INF/relatorios/Verbetes.zip
	 */
	public FabricaRelatorio(String pathZip){
		carregarArquivoRelativo(pathZip);
	}

	private void carregarArquivoAbsoluto(String pathZip) {
		fileZip = new File(pathZip);
	}

	private void carregarArquivoRelativo(String pathZip) {
		ServletContext context = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
		String fullPath = context.getRealPath(pathZip);
		fileZip = new File(fullPath);
	}
	

	
	/**
	 * 
	 * @param relZip          -> representa o arquivo zip contendo todos os jrxml e imagens necessários. <br/>
	 * @param parametros      -> contém a lista de parâmetros esperados pelo jrxml. <br/>
	 * @param listaResultado  -> contém a lista de objetos bean mapeadas pela tabela. <br/> 
	 * @param TipoRelatorio   -> 1-PDF, 2-XLS
	 * @return                -> retorna o byte[] que representa o relatório gerado.
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes") 
	public byte[] executarRelatorio(byte[] relZip, Map<String, Object> parametros, List listaResultado, TipoRelatorio tipoRelatorio) throws Exception {
		return gerarRelatorio(relZip, parametros, listaResultado, tipoRelatorio);
	}

	/**
	 * 
	 * @param relZip          -> representa o arquivo zip contendo todos os jrxml e imagens necessários. <br/>
	 * @param listaResultado  -> contém a lista de objetos bean mapeadas pela tabela. <br/> 
	 * @param TipoRelatorio   -> 1-PDF, 2-XLS
	 * @return                -> retorna o byte[] que representa o relatório gerado.
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes") 
	public byte[] executarRelatorio(byte[] relZip, List listaResultado, TipoRelatorio tipoRelatorio) throws Exception {
		return gerarRelatorio(relZip, null, listaResultado, tipoRelatorio);
	}
	
	
	/**
	 * Gera o relatório
	 * @param listaResultado  -> contém a lista de objetos bean mapeadas pela tabela. <br/> 
	 * @param TipoRelatorio   -> 1-PDF, 2-XLS
	 * @return                -> retorna o byte[] que representa o relatório gerado.
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	public byte[] executarRelatorio(List listaResultado, TipoRelatorio tipoRelatorio) throws Exception {
		if(fileZip == null) throw new Exception("O arquivo ZIP contendo o relatórios não foi informado.");
		
		//No caso da lista de parâmetros estiver nula.
		Map<String, Object> parametros = new LinkedHashMap<String, Object>();
		return gerarRelatorio(FileUtils.readFileToByteArray(fileZip) , parametros, listaResultado, tipoRelatorio);
	}
	
	
	/**
	 * Gera o relatório
	 * @param parametros      -> contém a lista de parâmetros esperados pelo jrxml. <br/>
	 * @param listaResultado  -> contém a lista de objetos bean mapeadas pela tabela. <br/> 
	 * @param TipoRelatorio   -> 1-PDF, 2-XLS
	 * @return                -> retorna o byte[] que representa o relatório gerado.
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	public byte[] executarRelatorio(Map<String, Object> parametros, List listaResultado, TipoRelatorio tipoRelatorio) throws Exception {
		if(fileZip == null) throw new Exception("O arquivo ZIP contendo o relatórios não foi informado.");
		
		return gerarRelatorio(FileUtils.readFileToByteArray(fileZip) , parametros, listaResultado, tipoRelatorio);
	}
	
	/**
	 * Apaga o diretório temporário após a visualização do relatório.           <br/>
	 * Isso é apenas para não ficar vários diretórios na pasta TMP do servidor. <br/>
	 */
	private void excluirDiretorioTemporario(){
		if(!FileUtils.deleteQuietly(tmpDir)){
			System.out.println("Não foi possível apagar o diretório: " + tmpDir.getAbsolutePath() );
		}
	}
	
	
	/**
	 * Gera o relatório
	 * @param relZip          -> representa o arquivo zip contendo todos os jrxml e imagens necessários. <br/>
	 * @param parametros      -> contém a lista de parâmetros esperados pelo jrxml. <br/>
	 * @param listaResultado  -> contém a lista de objetos bean mapeadas pela tabela. <br/> 
	 * @param tipoRelatorio   -> 1-PDF, 2-XLS
	 * @return                -> retorna o byte[] que representa o relatório gerado.
	 * @throws Exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" }) 
	private byte[] gerarRelatorio(byte[] relZip, Map<String, Object> parametros, List listaResultado, TipoRelatorio tipoRelatorio) throws Exception {
		//No caso da lista de parâmetros estiver nula
		if(parametros == null){parametros = new LinkedHashMap<String, Object>();}
		

		// Cria o diretório temporário onde ficará os arquivos jrxml, jasper e as imagens
		tmpDir = File.createTempFile("tmp","RelatorioDir");
		tmpDir.delete();
		tmpDir.mkdirs();
		
		parametros.put("P_PATH_COMPILADO", tmpDir.getAbsolutePath());

		// Descompacta os arquivos que estão no banco de dados.
		ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(relZip));
		ZipEntry ze = zis.getNextEntry();

		while (ze != null) {
			String arquivo = ze.getName();
			File novoArquivo = new File(tmpDir.getAbsolutePath() + File.separator + arquivo);
			new File(novoArquivo.getParent()).mkdirs();
			//System.out.println("Descomprimindo: " + arquivo + "....");

			FileOutputStream fos = new FileOutputStream(novoArquivo);

			int len;
			byte[] buffer = new byte[1024];
			while ((len = zis.read(buffer)) > 0) {
				fos.write(buffer,0,len);
			}
			fos.close();

			// Se for o último arquivo, então a variável ze será null, o que
			// fará sair do while
			ze = zis.getNextEntry();
		}

		JasperReport jasperReport = compileReport(tmpDir.getAbsolutePath()+ File.separator,"index");

		/////////////////////////////////////////////////////////////////////////////////
		//Mapeia o local onde estará os SUBRELATORIOS
		parametros.put("SUBREPORT_DIR",tmpDir.getAbsolutePath() + File.separator);
		/////////////////////////////////////////////////////////////////////////////////

		
		JasperPrint jasperPrint = null;
		
		//A configuração do parâmetro REPORT_CONNETION é apenas para evitar essas mensagem: 
		//  WARN query.JRJdbcQueryExecuter: The supplied java.sql.Connection object is null.
		if(getJndiNameDataSource() != null && !getJndiNameDataSource().isEmpty() ){
			/////////////////////////////////////////////////////////////////////////////////
			//Mapeia a conexão do parâmetro REPORT_CONNECTION
			initCtx = new InitialContext();
			DataSource dataSource = (DataSource) initCtx.lookup(getJndiNameDataSource().trim());
			conOracle = dataSource.getConnection();
			parametros.put("REPORT_CONNECTION", conOracle);
			/////////////////////////////////////////////////////////////////////////////////
			
			
			//Gera o relatório a partir de uma conexão, nesse caso é necessário informar o SELECT no jrxml
			jasperPrint = JasperFillManager.fillReport(jasperReport,parametros,conOracle);
		}else{
			//Gera o relatório a partir de um ResultSet, nesse caso o SELECT no jrxml não será usado.
			//jasperPrint = JasperFillManager.fillReport(jasper,parametros,new JRResultSetDataSource(rs));
			
			//Gera o relatório a partir de uma Lista de Bean, nesse caso o SELECT no jrxml não será usado.
			JRBeanCollectionDataSource jrBeanColDs = new JRBeanCollectionDataSource(listaResultado);
			jasperPrint = JasperFillManager.fillReport(jasperReport,parametros,jrBeanColDs);
		}
		
		Exporter exporter = null;
		
		switch (tipoRelatorio) {
		case PDF:
			exporter = new JRPdfExporter();
			break;
		case XLS:
			exporter = new JRXlsExporter();
			break;
		default:
			throw new Exception("Não é possível gerar o tipo de relatório informado (" + tipoRelatorio + "). Suportado apenas:[1-PDF, 2-XLS]");
		}
		exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
		
		ByteArrayOutputStream baosReport = new ByteArrayOutputStream();
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(baosReport));
		exporter.exportReport();
		
		byte[] toReturn = baosReport.toByteArray();
		baosReport.close();
		
		//Apaga o diretório temporário após a visualização do relatório
		excluirDiretorioTemporario();
		
		//Fecha a conexão
		if(conOracle != null && !conOracle.isClosed())
			conOracle.close();
		if(initCtx != null)
			initCtx.close();

		return toReturn;
	}

	
	
	/**
	 * Compila o relatório e seus sub-relatórios.
	 * 
	 * @param reportPath
	 * @param reportName
	 * @return
	 * @throws Throwable
	 */
	private JasperReport compileReport(final String reportPath, String reportName) throws Exception {
		JasperDesign jasperDesign = JRXmlLoader.load(reportPath + reportName+ ".jrxml");
		JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
		
		//Usado quando o relatório é salvo no disk ou quando enviamos pela network
		JRSaver.saveObject(jasperReport,reportPath + reportName + ".jasper");

		// Compilando os sub-relatórios
		JRElementsVisitor.visitReport(jasperReport,
				new JRVisitor() {
					@Override
					public void visitSubreport(JRSubreport subreport) {
						try {
							String expression = subreport.getExpression().getText().replace(".jasper","");
							StringTokenizer st = new StringTokenizer(expression,"\"/");

							String subReportName = null;
							while (st.hasMoreElements()) {
								subReportName = st.nextToken();
							}
							if (completedSubReport.contains(subReportName))
								return;
							completedSubReport.add(subReportName);
							compileReport(reportPath,subReportName);
						} catch (Throwable e) {
							subReportExceptioon = e;
						}
					}

					@Override
					public void visitTextField(JRTextField arg0) {}
					@Override
					public void visitStaticText(JRStaticText arg0) {}
					@Override
					public void visitRectangle(JRRectangle arg0) {}
					@Override
					public void visitLine(JRLine arg0) {}
					@Override
					public void visitImage(JRImage arg0) {}
					@Override
					public void visitGenericElement(JRGenericElement arg0) {}
					@Override
					public void visitFrame(JRFrame arg0) {}
					@Override
					public void visitEllipse(JREllipse arg0) {}
					@Override
					public void visitElementGroup(JRElementGroup arg0) {}
					@Override
					public void visitCrosstab(JRCrosstab arg0) {}
					@Override
					public void visitComponentElement(JRComponentElement arg0) {}
					@Override
					public void visitChart(JRChart arg0) {}
					@Override
					public void visitBreak(JRBreak arg0) {}

				});

		if (subReportExceptioon != null)
			throw new RuntimeException(subReportExceptioon);
		return jasperReport;
	}

	/**
	 * Nome do JNDI-NAME definido no DataSource. <br/>
	 * Exemplo: "java:jboss/bdintapp"
	 * @return
	 */
	public String getJndiNameDataSource() {
		return jndiNameDataSource;
	}

	public void setJndiNameDataSource(String jndiNameDataSource) {
		this.jndiNameDataSource = jndiNameDataSource;
	}
	
	
}
