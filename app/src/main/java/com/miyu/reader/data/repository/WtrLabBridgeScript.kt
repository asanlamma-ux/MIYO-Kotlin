package com.miyu.reader.data.repository

object WtrLabBridgeScript {
    const val START_URL = "https://wtr-lab.com/en/novel-finder"

    val bootstrap: String = """
(function () {
  if (window.__MIYO_WTR_BRIDGE_READY) {
    window.__MIYO_WTR_BRIDGE.postReady();
    true;
    return;
  }

  function post(payload) {
    if (!window.AndroidWtrBridge) return;
    window.AndroidWtrBridge.postMessage(JSON.stringify(payload));
  }

  function pageTextSample() {
    try { return String((document.body && document.body.innerText) || '').slice(0, 900); } catch (_error) { return ''; }
  }

  function isChallengePage() {
    var title = String(document.title || '').toLowerCase();
    var body = pageTextSample().toLowerCase();
    return title.indexOf('just a moment') !== -1 ||
      body.indexOf('security verification') !== -1 ||
      body.indexOf('captcha') !== -1 ||
      body.indexOf('verify you are human') !== -1 ||
      body.indexOf('protect against malicious bots') !== -1 ||
      body.indexOf('checking if the site connection is secure') !== -1;
  }

  function cleanText(value) {
    return String(value || '')
      .replace(/Auto generated hidden content[\s\S]*${'$'}/i, '')
      .replace(/\s+/g, ' ')
      .trim();
  }

  function stripHtml(html) {
    if (!html) return '';
    var doc = new DOMParser().parseFromString(String(html), 'text/html');
    return cleanText((doc.body && doc.body.textContent) || '');
  }

  function sanitizeHtml(html) {
    return String(html || '')
      .replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi, '')
      .replace(/<style\b[^>]*>[\s\S]*?<\/style>/gi, '')
      .replace(/<iframe\b[^>]*>[\s\S]*?<\/iframe>/gi, '')
      .replace(/<ins\b[^>]*>[\s\S]*?<\/ins>/gi, '')
      .replace(/\son[a-z]+\s*=\s*"[^"]*"/gi, '')
      .replace(/\son[a-z]+\s*=\s*'[^']*'/gi, '')
      .replace(/\son[a-z]+\s*=\s*[^\s>]+/gi, '')
      .replace(/\s(href|src)\s*=\s*"javascript:[^"]*"/gi, ' ${'$'}1="#"')
      .replace(/\s(href|src)\s*=\s*'javascript:[^']*'/gi, " ${'$'}1='#'")
      .replace(/Auto generated hidden content[\s\S]*${'$'}/i, '');
  }

  function escapeHtml(value) {
    return String(value || '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function asText(value) {
    if (value == null) return '';
    if (typeof value === 'string') return cleanText(value);
    if (typeof value === 'number') return String(value);
    if (typeof value === 'object') {
      if (typeof value.text === 'string') return cleanText(value.text);
      if (typeof value['#text'] === 'string') return cleanText(value['#text']);
    }
    return '';
  }

  function ensureArray(value) {
    if (Array.isArray(value)) return value;
    if (value == null) return [];
    return [value];
  }

  function statusLabel(value) {
    if (value === 0 || value === '0') return 'Ongoing';
    if (value === 1 || value === '1') return 'Completed';
    if (value === 2 || value === '2') return 'Hiatus';
    var text = cleanText(value);
    return text ? text.charAt(0).toUpperCase() + text.slice(1) : 'Unknown';
  }

  function parseNumber(value) {
    var match = String(value || '').replace(/,/g, '').match(/(\d+(?:\.\d+)?)/);
    return match ? Number(match[1]) : null;
  }

  function pickChapterCount(source) {
    var candidates = [
      source && source.chapter_count,
      source && source.chapterCount,
      source && source.total_chapters,
      source && source.totalChapters,
      source && source.chapters_count,
      source && source.count_chapter,
      source && source.chapter,
      source && source.total
    ];
    for (var i = 0; i < candidates.length; i += 1) {
      var parsed = Number(candidates[i]);
      if (!Number.isNaN(parsed) && parsed > 0) return parsed;
    }
    return null;
  }

  function pickRating(source) {
    var candidates = [source && source.rating, source && source.score, source && source.average_rating, source && source.avg_rating];
    for (var i = 0; i < candidates.length; i += 1) {
      var parsed = Number(candidates[i]);
      if (!Number.isNaN(parsed) && parsed > 0) return parsed;
    }
    return null;
  }

  function absUrl(input) {
    if (!input) return null;
    try { return new URL(input, 'https://wtr-lab.com/').toString(); } catch (_error) { return input; }
  }

  function parsePath(input) {
    if (!input) return '';
    try { return new URL(input, 'https://wtr-lab.com/').pathname; } catch (_error) { return String(input); }
  }

  function normalizeText(value) {
    return cleanText(value).toLowerCase();
  }

  function matchesCombinedQuery(item, query) {
    if (!query) return true;
    var needle = normalizeText(query);
    var haystacks = [
      item && item.title,
      item && item.author,
      item && item.summary,
      item && item.slug,
      item && item.status,
      item && item.path,
      item && item.providerLabel,
      item && item.genres ? item.genres.join(' ') : '',
      item && item.tags ? item.tags.join(' ') : ''
    ];
    for (var i = 0; i < haystacks.length; i += 1) {
      if (normalizeText(haystacks[i]).indexOf(needle) !== -1) return true;
    }
    return false;
  }

  function dedupe(items) {
    var seen = {};
    var output = [];
    for (var i = 0; i < items.length; i += 1) {
      var item = items[i];
      var key = [item.providerId || '', item.rawId || '', item.path || '', item.title || ''].join('::');
      if (seen[key]) continue;
      seen[key] = true;
      output.push(item);
    }
    return output;
  }

  function applySearchFilters(items, payload) {
    var minChapters = payload && payload.minChapters != null && payload.minChapters !== '' ? Number(payload.minChapters) : null;
    var maxChapters = payload && payload.maxChapters != null && payload.maxChapters !== '' ? Number(payload.maxChapters) : null;
    var status = payload && payload.status ? String(payload.status) : 'all';
    var query = payload && payload.query ? String(payload.query).trim() : '';
    return items.filter(function (item) {
      var count = item.chapterCount;
      if (minChapters != null && (count == null || count < minChapters)) return false;
      if (maxChapters != null && count != null && count > maxChapters) return false;
      if (status !== 'all' && normalizeText(item.status) !== normalizeText(status)) return false;
      return matchesCombinedQuery(item, query);
    });
  }

  function sortItems(items, payload) {
    var orderBy = payload && payload.orderBy ? String(payload.orderBy) : 'update';
    var order = payload && payload.order === 'asc' ? 1 : -1;
    var copy = items.slice();
    copy.sort(function (a, b) {
      if (orderBy === 'name') return normalizeText(a.title).localeCompare(normalizeText(b.title)) * order;
      if (orderBy === 'chapter') return ((a.chapterCount || 0) - (b.chapterCount || 0)) * order;
      if (orderBy === 'rating') return ((a.rating || 0) - (b.rating || 0)) * order;
      return 0;
    });
    return copy;
  }

  function nextDataFromDocument(doc) {
    var node = doc && doc.getElementById('__NEXT_DATA__');
    if (!node || !node.textContent) return null;
    try { return JSON.parse(node.textContent); } catch (_error) { return null; }
  }

  function nextDataFromHtml(html) {
    return nextDataFromDocument(new DOMParser().parseFromString(html, 'text/html'));
  }

  function delay(ms) {
    return new Promise(function (resolve) { setTimeout(resolve, ms); });
  }

  async function fetchText(url, init) {
    var response = await fetch(url, Object.assign({
      credentials: 'include',
      headers: { accept: 'text/html,application/json,application/xhtml+xml' }
    }, init || {}));
    if (!response.ok) throw new Error('Request failed (' + response.status + ')');
    return response.text();
  }

  async function fetchJson(url, init) {
    var response = await fetch(url, Object.assign({
      credentials: 'include',
      headers: { accept: 'application/json,text/plain,*/*' }
    }, init || {}));
    if (!response.ok) throw new Error('Request failed (' + response.status + ')');
    return response.json();
  }

  async function getBuildId() {
    for (var attempt = 0; attempt < 20; attempt += 1) {
      var current = nextDataFromDocument(document);
      if (current && current.buildId) return current.buildId;
      await delay(650);
    }
    var html = await fetchText('https://wtr-lab.com/en/novel-finder');
    var parsed = nextDataFromHtml(html);
    if (parsed && parsed.buildId) return parsed.buildId;
    throw new Error('WTR-LAB did not finish loading novel finder data.');
  }

  function mapSeriesItem(item) {
    var data = (item && item.data) || item || {};
    var rawId = Number(item && (item.raw_id || item.rawId || data.raw_id || data.rawId || data.id || item.id));
    var slug = String((item && (item.slug || data.slug)) || '');
    return {
      providerId: 'wtr-lab',
      providerLabel: 'WTR-LAB',
      rawId: rawId,
      slug: slug,
      path: '/en/novel/' + rawId + '/' + slug,
      title: asText(data.title || (item && item.title) || slug || 'Untitled'),
      coverUrl: absUrl(data.image || data.cover || (item && item.image) || (item && item.cover)),
      author: asText(data.author || (item && item.author) || 'Unknown Author'),
      summary: cleanText(stripHtml(data.description || data.summary || data.synopsis || (item && item.description) || '')),
      status: statusLabel(data.status || (item && item.status)),
      chapterCount: pickChapterCount(data) || pickChapterCount(item),
      rating: pickRating(data) || pickRating(item)
    };
  }

  async function recentNovels(page, payload) {
    var json = await fetchJson('https://wtr-lab.com/api/home/recent', {
      method: 'POST',
      headers: { accept: 'application/json,text/plain,*/*', 'content-type': 'application/json' },
      body: JSON.stringify({ page: page })
    });
    var items = ensureArray(json && json.data).map(function (item) {
      return mapSeriesItem(item && item.serie ? item.serie : item);
    });
    items = sortItems(applySearchFilters(dedupe(items), payload), payload);
    return { items: items, page: page, hasMore: items.length >= 20, nextCursor: null };
  }

  async function finderSearch(payload) {
    var page = Number(payload && payload.page) || 1;
    var params = new URLSearchParams();
    params.set('page', String(page));
    params.set('orderBy', payload && payload.orderBy ? String(payload.orderBy) : 'update');
    params.set('order', payload && payload.order ? String(payload.order) : 'desc');
    var status = payload && payload.status ? String(payload.status) : 'all';
    if (status && status !== 'all') params.set('status', status);
    var query = payload && payload.query ? String(payload.query).trim() : '';
    if (query) params.set('text', query);
    if (payload && payload.minChapters != null && payload.minChapters !== '') params.set('minc', String(payload.minChapters));
    if (payload && payload.minRating != null && payload.minRating !== '') params.set('minr', String(payload.minRating));
    if (payload && payload.minReviewCount != null && payload.minReviewCount !== '') params.set('minrc', String(payload.minReviewCount));
    var buildId = await getBuildId();
    var json = await fetchJson('https://wtr-lab.com/_next/data/' + buildId + '/en/novel-finder.json?' + params.toString());
    var series = (((json || {}).pageProps || {}).series) || [];
    var items = sortItems(applySearchFilters(dedupe(series.map(mapSeriesItem)), payload), payload);
    return { items: items, page: page, hasMore: items.length >= 20, nextCursor: null };
  }

  async function searchWtr(payload) {
    var page = Number(payload && payload.page) || 1;
    if (payload && payload.latestOnly) return recentNovels(page, payload);
    return finderSearch(payload);
  }

  function chaptersFromNextData(doc) {
    var nd = nextDataFromDocument(doc);
    if (!nd) return [];
    var pp = nd.props && nd.props.pageProps;
    if (!pp) return [];
    var rawId = pp.rawId || (pp.serie && (pp.serie.raw_id || pp.serie.rawId));
    var slug = pp.slug || (pp.serie && pp.serie.slug) || '';
    var list = (pp.serie && pp.serie.chapters) || pp.chapters || pp.chapterList || [];
    if (!Array.isArray(list) || !list.length) return [];
    return list.map(function (ch, idx) {
      var order = Number(ch.chapter_no || ch.order || ch.chapterNo || idx + 1);
      return {
        order: order,
        title: asText(ch.title || ch.name || 'Chapter ' + order),
        path: '/en/novel/' + rawId + '/' + slug + '/chapter-' + order,
        updatedAt: ch.updated_at || ch.updatedAt || null
      };
    });
  }

  async function fetchAllChapters(rawId, slug, totalChapters, hintDoc) {
    var nextDataChapters = hintDoc ? chaptersFromNextData(hintDoc) : [];
    if (nextDataChapters.length > 0) return nextDataChapters.sort(function (a, b) { return a.order - b.order; });
    var batchSize = 250;
    var endpoints = [
      function (s, e) { return { url: 'https://wtr-lab.com/api/chapters/' + rawId + '?start=' + s + '&end=' + e, method: 'GET', body: null }; },
      function (s, e) { return { url: 'https://wtr-lab.com/api/serie/' + rawId + '/chapters?start=' + s + '&end=' + e, method: 'GET', body: null }; },
      function (s, e) { return { url: 'https://wtr-lab.com/api/chapters/' + rawId + '?page=' + Math.ceil(s / batchSize) + '&limit=' + batchSize, method: 'GET', body: null }; }
    ];
    for (var epIdx = 0; epIdx < endpoints.length; epIdx += 1) {
      var output = [];
      var start = 1;
      for (var loops = 0; loops < 30; loops += 1) {
        var end = totalChapters ? Math.min(start + batchSize - 1, totalChapters) : start + batchSize - 1;
        var ep = endpoints[epIdx](start, end);
        try {
          var json = await fetchJson(ep.url, { method: ep.method });
          var raw = (Array.isArray(json) ? json : null) || json.chapters || json.list || json.items || (json.data && Array.isArray(json.data) ? json.data : null) || (json.data && json.data.chapters ? json.data.chapters : null) || [];
          raw = ensureArray(raw);
          if (!raw.length) break;
          raw.forEach(function (ch, idx) {
            var order = Number(ch.order || ch.chapter_no || ch.chapterNumber || ch.no || ch.chapter || start + idx);
            output.push({
              order: order,
              title: asText(ch.title || ch.name || ch.chapter_title || 'Chapter ' + order),
              path: '/en/novel/' + rawId + '/' + slug + '/chapter-' + order,
              updatedAt: ch.updated_at || ch.updatedAt || ch.update_time || null
            });
          });
          if (raw.length < batchSize || (totalChapters && end >= totalChapters)) break;
          start += batchSize;
          await delay(250);
        } catch (_error) {
          break;
        }
      }
      if (output.length) return output.sort(function (a, b) { return a.order - b.order; });
    }
    return [];
  }

  function parseGenres(doc, label) {
    var output = [];
    var hrefHints = label === 'genre' ? ['/genre/', '/genres/', '/theme/', '/themes/'] : ['/tag/', '/tags/'];
    var links = doc.querySelectorAll('a[href]');
    for (var i = 0; i < links.length; i += 1) {
      var href = String(links[i].getAttribute('href') || '').toLowerCase();
      var matches = hrefHints.some(function (hint) { return href.indexOf(hint) !== -1; });
      if (!matches) continue;
      var value = asText(links[i].textContent || '');
      if (value && output.indexOf(value) === -1) output.push(value);
    }
    return output;
  }

  async function fetchWtrNovelDetails(payload) {
    var rawId = Number(payload && payload.rawId);
    var slug = String((payload && payload.slug) || '');
    var path = payload && payload.path ? String(payload.path) : '/en/novel/' + rawId + '/' + slug;
    var html = await fetchText('https://wtr-lab.com' + path);
    var doc = new DOMParser().parseFromString(html, 'text/html');
    var nextData = nextDataFromDocument(doc);
    var serieData = nextData && nextData.props && nextData.props.pageProps && nextData.props.pageProps.serie && nextData.props.pageProps.serie.serie_data
      ? nextData.props.pageProps.serie.serie_data
      : null;
    var data = serieData && serieData.data ? serieData.data : {};
    rawId = rawId || Number(serieData && serieData.raw_id);
    slug = slug || String((serieData && serieData.slug) || '');
    var chapterCount = pickChapterCount(serieData) || pickChapterCount(data);
    var titleNode = doc.querySelector('h1') || doc.querySelector('meta[property="og:title"]') || doc.querySelector('meta[name="twitter:title"]');
    var authorNode = doc.querySelector('[itemprop="author"]') || doc.querySelector('[rel="author"]') || doc.querySelector('.author a') || doc.querySelector('.author') || doc.querySelector('.novel-author');
    var summaryNode = doc.querySelector('[itemprop="description"]') || doc.querySelector('.summary') || doc.querySelector('.lead') || doc.querySelector('meta[property="og:description"]') || doc.querySelector('meta[name="twitter:description"]') || doc.querySelector('meta[name="description"]');
    var coverNode = doc.querySelector('meta[property="og:image"]') || doc.querySelector('meta[name="twitter:image"]') || doc.querySelector('.cover img') || doc.querySelector('img[src*="/cover"]') || doc.querySelector('img');
    var details = {
      providerId: 'wtr-lab',
      providerLabel: 'WTR-LAB',
      rawId: rawId,
      slug: slug,
      path: path,
      title: asText(data.title || (titleNode && (titleNode.getAttribute('content') || titleNode.textContent)) || (payload && payload.fallbackTitle) || '') || 'Untitled',
      coverUrl: absUrl(data.image || data.cover || (coverNode && (coverNode.getAttribute('content') || coverNode.getAttribute('data-src') || coverNode.getAttribute('src'))) || (payload && payload.fallbackCoverUrl) || ''),
      author: asText(data.author || (authorNode && (authorNode.getAttribute('content') || authorNode.textContent)) || (payload && payload.fallbackAuthor) || '') || 'Unknown Author',
      summary: cleanText(stripHtml(data.description || data.summary || data.synopsis || (summaryNode && (summaryNode.getAttribute('content') || summaryNode.innerHTML)) || (payload && payload.fallbackSummary) || '')),
      status: statusLabel((serieData && serieData.status) || data.status || (payload && payload.fallbackStatus)),
      chapterCount: chapterCount || Number(payload && payload.fallbackChapterCount) || null,
      rating: pickRating(data) || pickRating(serieData),
      genres: parseGenres(doc, 'genre'),
      tags: parseGenres(doc, 'tag'),
      chapters: []
    };
    if (payload && payload.includeChapters === false) return details;
    details.chapters = await fetchAllChapters(rawId, slug, chapterCount, doc);
    if (!details.chapterCount) details.chapterCount = details.chapters.length;
    return details;
  }

  async function decryptPayload(encrypted, encKey) {
    var expectsArray = false;
    var payload = String(encrypted || '');
    if (payload.indexOf('arr:') === 0) {
      expectsArray = true;
      payload = payload.slice(4);
    } else if (payload.indexOf('str:') === 0) {
      payload = payload.slice(4);
    }
    var parts = payload.split(':');
    if (parts.length !== 3) throw new Error('Invalid encrypted data format.');
    var iv = Uint8Array.from(atob(parts[0]), function (char) { return char.charCodeAt(0); });
    var tag = Uint8Array.from(atob(parts[1]), function (char) { return char.charCodeAt(0); });
    var ciphertext = Uint8Array.from(atob(parts[2]), function (char) { return char.charCodeAt(0); });
    var combined = new Uint8Array(ciphertext.length + tag.length);
    combined.set(ciphertext);
    combined.set(tag, ciphertext.length);
    var keyBytes = new TextEncoder().encode(String(encKey || '').slice(0, 32));
    var cryptoKey = await crypto.subtle.importKey('raw', keyBytes, { name: 'AES-GCM' }, false, ['decrypt']);
    var plain = await crypto.subtle.decrypt({ name: 'AES-GCM', iv: iv }, cryptoKey, combined);
    var decoded = new TextDecoder().decode(plain);
    return expectsArray ? JSON.parse(decoded) : decoded;
  }

  async function fetchEncryptionKey(doc) {
    var searchKey = 'TextEncoder().encode("';
    var seen = {};
    var scripts = Array.prototype.slice.call((doc && doc.querySelectorAll('head script[src]')) || []);
    for (var index = 0; index < scripts.length; index += 1) {
      var src = String(scripts[index].getAttribute('src') || '');
      if (!src || seen[src]) continue;
      seen[src] = true;
      try {
        var raw = await fetchText(absUrl(src));
        var keyIndex = raw.indexOf(searchKey);
        if (keyIndex >= 0) {
          return raw.substring(keyIndex + searchKey.length, keyIndex + searchKey.length + 32);
        }
      } catch (_error) {
        // Ignore script fetch failures and keep scanning.
      }
    }
    throw new Error('Failed to find the chapter encryption key.');
  }

  async function translateLines(lines) {
    if (!Array.isArray(lines) || !lines.length) return [];
    var wrapped = lines.map(function (line, index) {
      return '<a i="' + index + '">' + escapeHtml(String(line || '')) + '</a>';
    });
    var response = await fetch('https://translate-pa.googleapis.com/v1/translateHtml', {
      credentials: 'omit',
      headers: {
        'content-type': 'application/json+protobuf',
        'X-Goog-API-Key': 'AIzaSyATBXajvzQLTDHEQbcpq0Ihe0vWDHmO520'
      },
      referrer: 'https://wtr-lab.com/',
      body: '[[' + JSON.stringify(wrapped) + ',"zh-CN","en"],"te_lib"]',
      method: 'POST'
    });
    if (!response.ok) throw new Error('On-device translation fallback failed (' + response.status + ').');
    var payload = await response.json();
    return Array.isArray(payload && payload[0]) ? payload[0] : lines;
  }

  function contentToHtml(content, glossary) {
    var replacements = {};
    if (glossary && glossary.terms && Array.isArray(glossary.terms)) {
      for (var i = 0; i < glossary.terms.length; i += 1) replacements['※' + i + '⛬'] = glossary.terms[i][0];
    }
    if (Array.isArray(content)) {
      return content.map(function (line) {
        var next = String(line);
        Object.keys(replacements).forEach(function (token) { next = next.split(token).join(replacements[token]); });
        return '<p>' + escapeHtml(next) + '</p>';
      }).join('');
    }
    var text = String(content || '');
    if (text.indexOf('<') >= 0) return sanitizeHtml(text);
    return '<p>' + escapeHtml(text) + '</p>';
  }

  async function fetchWtrChapter(payload) {
    var rawId = Number(payload && payload.rawId);
    var slug = String((payload && payload.slug) || '');
    var chapterNo = Number(payload && payload.chapterNo);
    var title = String((payload && payload.chapterTitle) || ('Chapter ' + chapterNo));
    var path = payload && payload.path ? String(payload.path) : '/en/novel/' + rawId + '/' + slug + '/chapter-' + chapterNo;
    var translationTypes = ['web', 'ai'];
    var result = null;
    var readerError = '';
    var fallbackNote = '';
    var chapterDoc = null;
    for (var i = 0; i < translationTypes.length; i += 1) {
      var response = await fetch('https://wtr-lab.com/api/reader/get', {
        method: 'POST',
        credentials: 'include',
        headers: { accept: 'application/json,text/plain,*/*', 'content-type': 'application/json' },
        referrer: 'https://wtr-lab.com' + path,
        body: JSON.stringify({
          translate: translationTypes[i],
          language: 'en',
          raw_id: rawId,
          chapter_no: chapterNo,
          retry: false,
          force_retry: false
        })
      });
      result = await response.json();
      if (response.ok && !(result && result.error)) break;
      readerError = result && (result.error || result.message) ? String(result.error || result.message) : readerError;
    }
    if (!result || result.success === false) throw new Error(readerError || 'Could not fetch the requested chapter.');
    var content = result.data && result.data.data ? result.data.data.body : null;
    var glossary = result.data && result.data.data ? result.data.data.glossary_data : null;
    if (typeof content === 'string' && (content.indexOf('arr:') === 0 || content.indexOf('str:') === 0)) {
      try {
        if (!chapterDoc) {
          var chapterHtml = await fetchText('https://wtr-lab.com' + path, {
            headers: { accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8' }
          });
          chapterDoc = new DOMParser().parseFromString(chapterHtml, 'text/html');
        }
        var encKey = await fetchEncryptionKey(chapterDoc);
        var decrypted = await decryptPayload(content, encKey);
        if (Array.isArray(decrypted)) {
          content = await translateLines(decrypted);
          fallbackNote = '<p><small>This chapter was decrypted locally and translated inside the external source runtime.</small></p>';
        } else {
          content = decrypted;
          fallbackNote = '<p><small>This chapter was decrypted locally inside the external source runtime.</small></p>';
        }
      } catch (error) {
        readerError = readerError || (error && error.message ? error.message : String(error));
        content = '<p><em>This WTR-LAB chapter returned an encrypted payload. Complete verification again or try the web translation mode later.</em></p>';
      }
    }
    var html = fallbackNote + contentToHtml(content, glossary);
    if (readerError) html = '<p><small>' + readerError + '</small></p>' + html;
    return { order: chapterNo, title: title, html: sanitizeHtml(html) };
  }

  async function fallbackChapterFromPage(payload, reason) {
    var chapterNo = Number(payload && payload.chapterNo);
    var title = String((payload && payload.chapterTitle) || ('Chapter ' + chapterNo));
    var rawId = Number(payload && payload.rawId);
    var slug = String((payload && payload.slug) || '');
    var path = payload && payload.path ? String(payload.path) : '/en/novel/' + rawId + '/' + slug + '/chapter-' + chapterNo;
    var html = await fetchText('https://wtr-lab.com' + path, {
      headers: { accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8' }
    });
    var doc = new DOMParser().parseFromString(html, 'text/html');
    var titleNode = doc.querySelector('h1') || doc.querySelector('h2') || doc.querySelector('meta[property="og:title"]');
    var content = doc.querySelector('article') ||
      doc.querySelector('[data-reader-content]') ||
      doc.querySelector('.reader-content') ||
      doc.querySelector('.chapter-content') ||
      doc.querySelector('.prose') ||
      doc.querySelector('main') ||
      doc.body;
    if (!content) throw new Error(reason || 'Could not fetch the requested chapter.');
    var cloned = content.cloneNode(true);
    Array.prototype.slice.call(cloned.querySelectorAll('script,style,iframe,ins,nav,header,footer,.ads,.ad,.comments')).forEach(function (node) {
      if (node && node.parentNode) node.parentNode.removeChild(node);
    });
    var bodyHtml = sanitizeHtml(cloned.innerHTML || '');
    if (!stripHtml(bodyHtml)) throw new Error(reason || 'Could not fetch the requested chapter.');
    var pageTitle = asText(titleNode && (titleNode.getAttribute('content') || titleNode.textContent)) || title;
    return {
      order: chapterNo,
      title: pageTitle,
      html: '<p><small>Recovered from the WTR-LAB reader page after API fallback.</small></p>' + bodyHtml
    };
  }

  async function fetchWtrChapterRobust(payload) {
    var lastError = '';
    for (var attempt = 0; attempt < 3; attempt += 1) {
      try {
        return await fetchWtrChapter(payload);
      } catch (error) {
        lastError = error && error.message ? error.message : String(error);
        await delay(500 + attempt * 650);
      }
    }
    return fallbackChapterFromPage(payload, lastError);
  }

  async function fetchWtrChaptersBatch(payload, requestId) {
    var chapters = ensureArray(payload && payload.chapters);
    if (!chapters.length) throw new Error('No WTR-LAB chapters were selected.');
    var concurrency = Math.max(2, Math.min(Number(payload && payload.maxConcurrency) || 4, 10));
    var cursor = 0;
    var completed = 0;
    var results = new Array(chapters.length);
    var failures = [];
    function postProgress(chapterNo) {
      completed += 1;
      if (!requestId) return;
      post({
        scope: 'wtr-lab',
        type: 'progress',
        id: requestId,
        providerId: 'wtr-lab',
        payload: {
          completed: completed,
          total: chapters.length,
          chapterNo: chapterNo
        }
      });
    }
    async function worker() {
      while (cursor < chapters.length) {
        var index = cursor;
        cursor += 1;
        var chapter = chapters[index] || {};
        var request = Object.assign({}, payload, {
          chapterNo: Number(chapter.order || chapter.chapterNo || index + 1),
          chapterTitle: String(chapter.title || ('Chapter ' + (index + 1))),
          path: chapter.path || payload.path
        });
        try {
          results[index] = await fetchWtrChapterRobust(request);
        } catch (error) {
          failures.push('Chapter ' + request.chapterNo + ': ' + (error && error.message ? error.message : String(error)));
          results[index] = {
            order: request.chapterNo,
            title: request.chapterTitle,
            html: '<p><em>Chapter download failed after retries: ' + escapeHtml(error && error.message ? error.message : String(error)) + '</em></p>'
          };
        } finally {
          postProgress(request.chapterNo);
        }
      }
    }
    var workers = [];
    for (var i = 0; i < Math.min(concurrency, chapters.length); i += 1) workers.push(worker());
    await Promise.all(workers);
    var downloaded = results.filter(Boolean).sort(function (a, b) { return a.order - b.order; });
    if (!downloaded.length) throw new Error(failures[0] || 'No chapters could be downloaded.');
    return {
      chapters: downloaded,
      failedCount: failures.length,
      failures: failures.slice(0, 8)
    };
  }

  async function executeRequest(request) {
    if (isChallengePage()) throw new Error('Verification is required before WTR-LAB can load inside Miyo.');
    var payload = request && request.payload ? request.payload : {};
    if (request.type === 'search') return searchWtr(payload);
    if (request.type === 'details') return fetchWtrNovelDetails(payload);
    if (request.type === 'chapter') return fetchWtrChapterRobust(payload);
    if (request.type === 'chapters') return fetchWtrChaptersBatch(payload, request && request.id);
    throw new Error('Unsupported WTR-LAB bridge request.');
  }

  window.__MIYO_WTR_BRIDGE = {
    postReady: function () {
      if (isChallengePage()) {
        post({ scope: 'wtr-lab', type: 'challenge', providerId: 'wtr-lab', title: document.title || 'Verification required', body: pageTextSample() });
      } else {
        post({ scope: 'wtr-lab', type: 'ready', providerId: 'wtr-lab' });
      }
    },
    run: async function (request) {
      try {
        var result = await executeRequest(request);
        post({ scope: 'wtr-lab', type: 'result', id: request.id, providerId: 'wtr-lab', payload: result });
      } catch (error) {
        post({ scope: 'wtr-lab', type: 'error', id: request && request.id, providerId: 'wtr-lab', error: error && error.message ? error.message : String(error) });
      }
    }
  };
  window.__MIYO_WTR_BRIDGE_READY = true;
  window.__MIYO_WTR_BRIDGE.postReady();
})();
true;
""".trimIndent()
}
