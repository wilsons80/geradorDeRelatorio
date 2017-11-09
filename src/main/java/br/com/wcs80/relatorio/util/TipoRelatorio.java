/**
 * Projeto   : GeradorDeRelatorio
 * Pacote    : br.jus.trt10.relatorio.util
 * Classe    : TipoRelatorio.java
 * Autor     : wilson.souza
 * Data      : 04/10/2016
 * Descrição : 
 * 
 * Modificações 
 * 
 * Responsavel  Data        Descrição
 * ===========  ==========  =====================================================               
 *
 */

package br.com.wcs80.relatorio.util;

public enum TipoRelatorio {
	
	PDF(1,".pdf"),
	XLS(2,".xls");

	private String extensao;
	private int    codArquivo;
	
	private TipoRelatorio(int pCodArquivo, String pExtensao){
		extensao = pExtensao;
		codArquivo = pCodArquivo;
	}

	public String getExtensao() {
		return extensao;
	}

	public void setExtensao(String extensao) {
		this.extensao = extensao;
	}

	public int getCodArquivo() {
		return codArquivo;
	}

	public void setCodArquivo(int codArquivo) {
		this.codArquivo = codArquivo;
	}

	
	
}


