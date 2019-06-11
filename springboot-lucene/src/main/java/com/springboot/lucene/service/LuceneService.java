package com.springboot.lucene.service;

import com.springboot.lucene.daomain.IndexObject;
import com.springboot.lucene.daomain.LuceneResponse;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TODO description
 * <p>
 *
 * @author booxj
 * @create 2019/6/11 15:00
 * @since
 */
@Service
public class LuceneService {

    private final static Logger log = LoggerFactory.getLogger(LuceneService.class);

    private Directory directory = null;
    private Analyzer analyzer = null;

    @Value("${lucene.document.path:./lucene/file}")
    private String indexDer;

    @PostConstruct
    public void init() {
        try {
            directory = FSDirectory.open(Paths.get(indexDer));
            analyzer = new SmartChineseAnalyzer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ��������
     *
     * @param indexObject
     */
    public void create(IndexObject indexObject) {
        IndexWriter indexWriter = null;
        try {
            IndexWriterConfig config = new IndexWriterConfig(this.analyzer);
            indexWriter = new IndexWriter(directory, config);

            indexWriter.addDocument(IndexObject2Document(indexObject));
            indexWriter.commit();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                indexWriter.rollback();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                indexWriter.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * ɾ������
     *
     * @param id
     */
    public void delete(Long id) {
        IndexWriter indexWriter = null;
        try {
            Term term = new Term("id", id.toString());
            IndexWriterConfig config = new IndexWriterConfig(this.analyzer);
            indexWriter = new IndexWriter(directory, config);

            indexWriter.deleteDocuments(term);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                indexWriter.rollback();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                indexWriter.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * ɾ������
     */
    public void deleteAll() {
        IndexWriter indexWriter = null;
        try {
            IndexWriterConfig config = new IndexWriterConfig(this.analyzer);
            indexWriter = new IndexWriter(directory, config);

            Long result = indexWriter.deleteAll();
            // ��ջ���վ
            indexWriter.forceMergeDeletes();
            log.info("deleted:{}", result);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                indexWriter.rollback();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                indexWriter.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * ���µ�������
     */
    public void update(IndexObject indexObject) {

        IndexWriter indexWriter = null;

        try {
            Term term = new Term("id", indexObject.getId().toString());
            IndexWriterConfig config = new IndexWriterConfig(this.analyzer);
            indexWriter = new IndexWriter(directory, config);

            indexWriter.updateDocument(term, IndexObject2Document(indexObject));
        } catch (Exception e) {
            e.printStackTrace();
            try {
                indexWriter.rollback();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                indexWriter.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public LuceneResponse page(Integer pageNumber, Integer pageSize, String keyword) {

        IndexReader indexReader = null;
        LuceneResponse luceneResponse = null;
        List<IndexObject> searchResults = new ArrayList<>();
        try {
            indexReader = DirectoryReader.open(directory);
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            Query query = query(keyword, analyzer, "title", "description");
            ScoreDoc lastScoreDoc = this.getLastScoreDoc(pageNumber, pageSize, query, indexSearcher);
            // ����һҳ�����һ��document���ݸ�searchAfter�����Եõ���һҳ�Ľ��
            TopDocs topDocs = indexSearcher.searchAfter(lastScoreDoc, query, pageSize);
            Highlighter highlighter = this.addStringHighlighter(query);
            log.info("�������{}", keyword);
            log.info("�ܹ��Ĳ�ѯ�����{}", topDocs.totalHits);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                int docID = scoreDoc.doc;
                float score = scoreDoc.score;
                Document document = indexSearcher.doc(docID);
                IndexObject indexObject = document2IndexObject(analyzer, highlighter, document, score);
                searchResults.add(indexObject);
                log.info("��ضȵ÷֣�" + score);
            }
            Collections.sort(searchResults);
            luceneResponse = LuceneResponse.ok(searchResults, topDocs.totalHits);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                indexReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return luceneResponse;
    }

    /**
     * ����ҳ��ͷ�ҳ��С��ȡ��һ�ε����һ��ScoreDoc
     *
     * @param pageNumber
     * @param pageSize
     * @param query
     * @param searcher
     * @return
     * @throws IOException
     */
    private ScoreDoc getLastScoreDoc(Integer pageNumber, Integer pageSize, Query query, IndexSearcher searcher) throws IOException {
        if (pageNumber == 1) {
            return null;
        }
        int total = pageSize * (pageNumber - 1);
        TopDocs topDocs = searcher.search(query, total);
        return topDocs.scoreDocs[total - 1];
    }

    /**
     * �����ַ�������
     *
     * @param query
     * @return
     */
    private Highlighter addStringHighlighter(Query query) {
        QueryScorer scorer = new QueryScorer(query);
        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer);
        SimpleHTMLFormatter simpleHTMLFormatter = new SimpleHTMLFormatter("<font color='red'>", "</font>");
        Highlighter highlighter = new Highlighter(simpleHTMLFormatter, scorer);
        highlighter.setTextFragmenter(fragmenter);
        return highlighter;
    }

    /**
     * �ؼ��ּ���
     *
     * @param analyzer
     * @param highlighter
     * @param document
     * @param field
     * @return
     * @throws Exception
     */
    private String stringFormatHighlighterOut(Analyzer analyzer, Highlighter highlighter, Document document, String field) throws Exception {
        String fieldValue = document.get(field);
        if (fieldValue != null) {
            TokenStream tokenStream = analyzer.tokenStream(field, new StringReader(fieldValue));
            return highlighter.getBestFragment(tokenStream, fieldValue);
        }
        return null;
    }

    private Query query(String query, Analyzer analyzer, String... fields) throws ParseException {
        BooleanQuery.setMaxClauseCount(32768);
        // ���˷Ƿ��ַ�
        query.replace("/", " ").replace("\\", " ");
        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
        parser.setDefaultOperator(QueryParser.Operator.OR);
        return parser.parse(query);
    }


    /**
     * <p>
     * new StringField ���ִ�(id,���֤��,�绰...)
     * new StoredField ���ִ�(����)
     * new TextField �ִ�(�ı�)
     * ����
     * </p>
     *
     * @param indexObject
     * @return
     */
    public static Document IndexObject2Document(IndexObject indexObject) {
        Document doc = new Document();
        doc.add(new StringField("id", indexObject.getId().toString(),Field.Store.YES));
        doc.add(new TextField("title", indexObject.getTitle(), Field.Store.YES));
        doc.add(new TextField("summary", indexObject.getKeywords(), Field.Store.YES));
        doc.add(new TextField("description", indexObject.getDescription(), Field.Store.YES));
        doc.add(new StoredField("createDate", indexObject.getCreateDate()));
        doc.add(new StoredField("url", indexObject.getUrl()));
        return doc;
    }

    private IndexObject document2IndexObject(Analyzer analyzer, Highlighter highlighter, Document doc, float score) throws Exception {
        IndexObject indexObject = new IndexObject();
        indexObject.setId(Long.parseLong(doc.get("id")));
        indexObject.setTitle(stringFormatHighlighterOut(analyzer, highlighter, doc, "title"));
        indexObject.setKeywords(stringFormatHighlighterOut(analyzer, highlighter, doc, "summary"));
        indexObject.setDescription(stringFormatHighlighterOut(analyzer, highlighter, doc, "description"));
        indexObject.setCreateDate(doc.get("createDate"));
        indexObject.setUrl(doc.get("url"));
        indexObject.setScore(score);
        return indexObject;
    }
}
