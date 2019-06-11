package com.springboot.lucene;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * TODO description
 * <p>
 *
 * @author booxj
 * @create 2019/6/11 14:13
 * @since
 */
@SpringBootTest
public class LuceneTest {

    private static final String LUCENE_DOCUMENT_LOCATION = "lucene_document";
    private static final String LUCENE_INDEX_LOCATION = "lucene_index";

    //��������
    @Test
    public void luceneCreateIndex() throws Exception {
        // ָ��������ŵ�λ��
        String resourcesPath = this.getClass().getResource("/").getPath().substring(1);
        Directory directory = FSDirectory.open(Paths.get(resourcesPath + LUCENE_INDEX_LOCATION));
        System.out.println("pathname: " + resourcesPath + LUCENE_INDEX_LOCATION);
        // ����һ���ִ���
        SmartChineseAnalyzer smartChineseAnalyzer = new SmartChineseAnalyzer();
        // ����indexwriterConfig(�����ִ���)
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(smartChineseAnalyzer);
        // ����indexwrite ����(�ļ������������ö���)
        IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);

        // ԭʼ�ļ�
        File file = new File(resourcesPath + LUCENE_DOCUMENT_LOCATION);
        for (File f : file.listFiles()) {
            String fileName = f.getName();
            // �ļ�����
            String fileContent = FileUtils.readFileToString(f, "UTF-8");
            System.out.println(fileContent);
            // �ļ�·��
            String path = f.getPath();
            // �ļ���С
            long fileSize = FileUtils.sizeOf(f);

            // �����ļ�����
            // ������� ������� �Ƿ�洢
            Field fileNameField = new TextField("fileName", fileName, Field.Store.YES);
            Field fileContentField = new TextField("fileContent", fileContent, Field.Store.YES);
            Field filePathField = new TextField("filePath", path, Field.Store.YES);
            Field fileSizeField = new TextField("fileSize", fileSize + "", Field.Store.YES);

            // ����Document ����
            Document indexableFields = new Document();
            indexableFields.add(fileNameField);
            indexableFields.add(fileContentField);
            indexableFields.add(filePathField);
            indexableFields.add(fileSizeField);
            // ������������д��������
            indexWriter.addDocument(indexableFields);
        }

        //�ر�indexWriter
        indexWriter.close();
    }

    @Test
    public void searchIndex() throws IOException {
        //ָ����������·��
        String resourcesPath = this.getClass().getResource("/").getPath().substring(1);

        Directory directory = FSDirectory.open(Paths.get(resourcesPath + LUCENE_INDEX_LOCATION));
        //����indexReader����
        IndexReader indexReader = DirectoryReader.open(directory);
        //����indexSearcher����
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        //������ѯ
        Query query = new TermQuery(new Term("fileContent", "֪��"));
        //ִ�в�ѯ
        //����һ  ��ѯ����    ������  ��ѯ������ص����ֵ
        TopDocs topDocs = indexSearcher.search(query, 10);
        System.out.println("��ѯ���������: " + topDocs.totalHits);
        //������ѯ���
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            //scoreDoc.doc ���Ծ���doucumnet�����id
            Document doc = indexSearcher.doc(scoreDoc.doc);
            System.out.println(doc.getField("fileName"));
            System.out.println(doc.getField("fileContent"));
            System.out.println(doc.getField("filePath"));
            System.out.println(doc.getField("fileSize"));
        }
        indexReader.close();
    }
}
