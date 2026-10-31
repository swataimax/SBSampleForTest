package com.example.demo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class DiaryController {
	
	@Autowired
	DiaryRepository repository;
	
	
	@GetMapping("/top")
	public String top(Model model) {
		List<DiaryEntity> list = repository.findAll();
		model.addAttribute("list", list);
		model.addAttribute("keyword", "");	// 2026/10/26 検索欄の初期値追加
		return "top";
	}
	
	@GetMapping("/write")
	public String write(Model model) {
		System.out.println("/write");
//		model.addAttribute("form", new DiaryEntity());
		DiaryEntity entity = new DiaryEntity();
		model.addAttribute("form", entity);
		System.out.println(entity);
		// DiaryEntity(id=null, date=null, title=null, memo=null, category=null, type=null, ids=null, filename=null)
		// DBに登録するまでIDはふられない
		List<String> types = List.of("メモ", "記録", "temp", "要保存");
		model.addAttribute("types", types);
		return "write";
	}
	
	@PostMapping("/insert")
	@Transactional(readOnly = false)
	public String insert(
			@RequestParam("file") MultipartFile file,
			@ModelAttribute("form") @Validated DiaryEntity entity,
			BindingResult result, 
			Model model) {
		System.out.println("/insert");
		System.out.println(entity);
		System.out.println(file.getOriginalFilename());
		
		if (result.hasErrors()) {
			if (entity.getId() == null) { // IDが未設定の場合
				return "write";
			} else { // IDが設定されている場合
				return "update";
			}
		}
		
		
		// 20261023　画像ファイル名にIDを付けるように修正
		// DBに登録しないとIDが決まらないので、先に登録して、その後に画像をコピーして、ファイル名を登録し直す
		System.out.println("DB登録前:");
		System.out.println(entity);			// id=null
		
		repository.saveAndFlush(entity);
		
		System.out.println("DB登録後:");
		System.out.println(entity);			// id=・・・ つまり、saveAndFlushの後にentityにidが入っている
		
		
		if (file.isEmpty()) {	// 画像ファイル未選択
			System.out.println("画像ファイル：未選択");
		} else {
			System.out.println("file: " + file.getOriginalFilename());
		}
		
		String filename = file.getOriginalFilename();
		filename = entity.getId() + "_" + filename;
		System.out.println(filename);
		
		if (!file.isEmpty()) {
			// 画像コピー
			// https://spring.pleiades.io/spring-framework/docs/current/javadoc-api/org/springframework/web/multipart/MultipartFile.html
			// String dstPath = "C:\\pleiades\\workspace\\SBSampleForTest\\src\\main\\resources\\static\\img\\" + file.getOriginalFilename();
			// 2026/05/25 相対パスでOK
//			String dstPath = "src\\main\\resources\\static\\img\\" + file.getOriginalFilename();
//			System.out.println(dstPath);
//			try (BufferedInputStream bis = new BufferedInputStream(file.getInputStream());
//					BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(dstPath));) {
//				for (int b; (b = bis.read()) != -1;) {
//					bos.write(b);
//				}
//			} catch (IOException e) {
//				e.getStackTrace();
//			}
			
			// 2026/05/25 nioのPath,Paths,Filesを使って画像コピー
			// https://docs.oracle.com/javase/jp/25/docs/api/java.base/java/nio/file/Files.html
			try {
				// Path path = Paths.get("/src/main/resources/static/img/" + file.getOriginalFilename());
				// Path path = Paths.get("src/main/resources/static/img/" + file.getOriginalFilename());
				Path path = Paths.get("src/main/resources/static/img/" + filename);
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.getStackTrace();
			}
			
			// 20261023修正
			System.out.println(entity); 	// filename=null
			// entity.setFilename(file.getOriginalFilename());
			entity.setFilename(filename);
			
			System.out.println(entity); 	// filename=・・・
		}
		
		
		repository.saveAndFlush(entity);
		
		return "redirect:/top";
//		List<DiaryEntity> list = repository.findAll();
//		model.addAttribute("list", list);
//		return "top";
		// 2026/10/25 ここをリダイレクトにしていないと、
		// 登録後のURLが「/insert」になる（「/top」ではなく）（POST宛先のまま）
		// （登録後の画面をブックマークしても意味のないブックマークになる）
		// 登録後に「F5(更新)」を押すとPOSTリクエストを再送信しようとして
		// 「フォームの再送信を確認」という警告ポップアップが表示され、
		// 「続行」を押すと、同じデータが再登録される
		
	}
	
	
	@GetMapping("/detail/{id}")
	public String detail(@PathVariable Integer id, Model model) {
		System.out.println("/detail/{id}");
		System.out.println(id);
		Optional<DiaryEntity> data = repository.findById(id);
		model.addAttribute("form", data.get()); // Optionalから取得
		return "detail";
	}
	
	@GetMapping("/update/{id}")
	public String update(
			@PathVariable Integer id, Model model) {
		Optional<DiaryEntity> data = repository.findById(id);
		model.addAttribute("form", data.get());
		return "update";  
	}
	
	@GetMapping("/delete/{id}")
	public String delete(@PathVariable Integer id, Model model) {
		Optional<DiaryEntity> data = repository.findById(id);
		model.addAttribute("form", data.get());
		return "delete";  
	}
	
	@PostMapping("/remove")
	@Transactional(readOnly = false)
	public String remove(
			@ModelAttribute("form") DiaryEntity entity) {
		System.out.println("/remove");
		int id = entity.getId();
		System.out.println(entity.getFilename());
		
		// 2026/05/25 ファイル削除処理を追加
		try {
			Path path = Paths.get("src/main/resources/static/img/" + entity.getFilename());
//			Files.delete(path);
			Files.deleteIfExists(path);
		} catch (IOException e) {
			e.getStackTrace();
		}
		
		repository.deleteById(id);
		
		return "redirect:/top";
	}
	
	
	@GetMapping("/search")
	public String search(
			@RequestParam("keyword") String key, Model model) {
		List<DiaryEntity> list = repository.findByTitleLike("%" + key + "%");
		model.addAttribute("list", list);
		model.addAttribute("keyword", key);	// 2026/10/26 検索欄の初期値追加
		return "top";
	}
	
	
	
	// ----- paging ----- ----- ----- ----- ----- ----- ----- ----- ----- -----
	
	// 2026/06/16 ページング
//		@GetMapping("/paging")
//		public String paging(
//				@PageableDefault(size = 5) Pageable pageable,
//				Model model) {
//			System.out.println("/paging");
//			Page<DiaryEntity> list = repository.findAll(pageable);
//			model.addAttribute("list", list);
//			return "paging";
//		}
	// 2026/10/25 ページングの別の方法
	@GetMapping("/paging")
	public String paging(
			@PageableDefault(size = 5) Pageable pageable,
			Model model) {
		System.out.println("/paging");
		System.out.println("getPageNumber=" + pageable.getPageNumber());	// 0スタート
		System.out.println("pageSize=" + pageable.getPageSize());
		System.out.println("Sort=" + pageable.getSort());
		Pageable newPageable = PageRequest.of(
				pageable.getPageNumber(),
				pageable.getPageSize());
		Page<DiaryEntity> list = repository.findAll(newPageable);
		model.addAttribute("list", list);
		return "paging";
	}
	
	
	
	// ----- sort ----- ----- ----- ----- ----- ----- ----- ----- ----- -----
	
	// 2026/10/24 ソート（並び替え）
	@GetMapping("/sort")
	public String sort(
			Model model) {
		System.out.println("/sort");
		List<DiaryEntity> list = repository.findAll();
		model.addAttribute("list", list);
		model.addAttribute("jun", "");
		model.addAttribute("sortForm", new SortForm());		// sort2のため
		return "sort";
	}
	
	@PostMapping("/sort1")
	public String sort1(
			@RequestParam("jun") String jun,
			Model model) {
		System.out.println("/sort1");
		System.out.println("jun=" + jun);
		List<DiaryEntity> list;
		if ("昇順".equals(jun)) {
			list = repository.findAllByOrderByTitleAsc();
		} else if ("降順".equals(jun)){
			list = repository.findAllByOrderByTitleDesc();	
		} else {
			list = repository.findAll();
		}
		model.addAttribute("list", list);
		model.addAttribute("jun", jun);
		model.addAttribute("sortForm", new SortForm());		// sort2のため
		return "sort";
	}
	
	@PostMapping("/sort2")
	public String sort2(
			@ModelAttribute("sortForm") SortForm sortForm,
			Model model) {
		System.out.println("/sort2");
		System.out.println("sortForm=" + sortForm);
		List<DiaryEntity> list;
		if ("昇順".equals(sortForm.getJun2())) {
			list = repository.findAll(Sort.by(Sort.Direction.ASC, "title"));
		} else if ("降順".equals(sortForm.getJun2())) {
			list = repository.findAll(Sort.by(Sort.Direction.DESC, "title"));	
		} else {
			list = repository.findAll(Sort.by(Sort.Direction.ASC, "id"));
		}
		model.addAttribute("list", list);
		model.addAttribute("sortForm", sortForm);
		return "sort";
	}
	
	@PostMapping("/sort3")
	public String sort3(
			@RequestParam("sortColumn") String sortColumn,
			@RequestParam("sortDirection") String sortDirection,
			Model model) {
		System.out.println("/sort3");
		System.out.println("sortColumn=" + sortColumn);
		System.out.println("sortDirection=" + sortDirection);
		if ("--".equals(sortDirection)) {
			sortDirection = "昇順";
		}
		Sort sort = "降順".equals(sortDirection) ?
				Sort.by(Sort.Direction.DESC, sortColumn) : Sort.by(Sort.Direction.ASC, sortColumn);	
		List<DiaryEntity> list = repository.findAll(sort);
		model.addAttribute("list", list);
		model.addAttribute("sortColumn", sortColumn);
		model.addAttribute("sortDirection", sortDirection);
		model.addAttribute("sortForm", new SortForm());		// sort2のため
		return "sort";
	}
	
	
	
	// ----- paging + sort ----- ----- ----- ----- ----- ----- ----- ----- ----- -----
	
	// 2026/10/25 ページング・ソート
	@GetMapping("/pagingsort")
	public String pagingsort(
			@PageableDefault(size = 5) Pageable pageable,
			HttpSession session,
			Model model) {
		System.out.println("/pagingsort");
		System.out.println("getPageNumber=" + pageable.getPageNumber());	// 0スタート
		System.out.println("pageSize=" + pageable.getPageSize());
		System.out.println("Sort=" + pageable.getSort());
//			Sort sort = Sort.by(Sort.Direction.ASC, "id").and(Sort.by(Sort.Direction.ASC, "date"));
//			Sort sort = Sort.by(Sort.Direction.ASC, "date");
		Page<DiaryEntity> list = repository.findAll(pageable);
		model.addAttribute("list", list);
		String sortColumn = "id";
		String sortDirection = "昇順";
		model.addAttribute("sortColumn", sortColumn);
		model.addAttribute("sortDirection", sortDirection);
		session.setAttribute("sortColumn", sortColumn);
		session.setAttribute("sortDirection", sortDirection);
		return "pagingsort";
	}
	
	@PostMapping("/pagingsort")
	public String pagingsortPost(
			@RequestParam("sortColumn") String sortColumn,
			@RequestParam("sortDirection") String sortDirection,
			HttpSession session,
			Model model) {
		System.out.println("/pagingsortPost");
		System.out.println("sortColumn=" + sortColumn);
		System.out.println("sortDirection=" + sortDirection);
		if ("--".equals(sortDirection)) {
			sortDirection = "昇順";
		}
		System.out.println("sortColumn=" + sortColumn);
		System.out.println("sortDirection=" + sortDirection);
		session.setAttribute("sortColumn", sortColumn);
		session.setAttribute("sortDirection", sortDirection);
		return "redirect:/pagingsort1";
	}
	
	@GetMapping("/pagingsort1")
	public String pagingsort1(
			@PageableDefault(size = 5) Pageable pageable,
			HttpSession session,
			Model model) {
		System.out.println("/pagingsort1");
		System.out.println("getPageNumber=" + pageable.getPageNumber());	// 0スタート
		System.out.println("pageSize=" + pageable.getPageSize());
		System.out.println("Sort=" + pageable.getSort());
		String sortColumn = (String)session.getAttribute("sortColumn");
		String sortDirection = (String)session.getAttribute("sortDirection");
		System.out.println("sortColumn=" + sortColumn);
		System.out.println("sortDirection=" + sortDirection);
		Sort sort = "降順".equals(sortDirection) ?
				Sort.by(Sort.Direction.DESC, sortColumn) : Sort.by(Sort.Direction.ASC, sortColumn);	
		Pageable newPageable = PageRequest.of(
				pageable.getPageNumber(),
				pageable.getPageSize(),
				sort);
		Page<DiaryEntity> list = repository.findAll(newPageable);
		model.addAttribute("list", list);
		model.addAttribute("sortColumn", sortColumn);
		model.addAttribute("sortDirection", sortDirection);
		return "pagingsort";
	}
	
	
	
	// ----- paging + sort ----- ----- ----- ----- ----- ----- ----- ----- ----- -----
	
	// 2026/10/25 一括削除
	@GetMapping("/bulkdelete")
	public String bulkdelete(
			Model model) {
		System.out.println("/bulkdelete");
		List<DiaryEntity> list = repository.findAll();
		model.addAttribute("list", list);
		return "bulkdelete";
	}
	
	@PostMapping("/bulkdelete")
	public String bulkdeletePost(
			@RequestParam("ids") List<Integer> ids,
			@RequestParam("deleteIds") List<Integer> deleteIds,
			Model model) {
		System.out.println("/bulkdeletePost");
		System.out.println("ids=" + ids);
		System.out.println("deleteIds=" + deleteIds);
		for (Integer id : deleteIds) {
			repository.deleteById(id);
		}
//			List<DiaryEntity> list = repository.findAll();
//			model.addAttribute("list", list);
//			return "bulkdelete";
		return "redirect:/bulkdelete";
	}
	
	
	
	// ----- CSV ----- ----- ----- ----- ----- ----- ----- ----- ----- -----
	
	// 2026/06/16
	@GetMapping("/csv")
	public String csv(
			Model model) {
		System.out.println("/csv");
		
		List<DiaryEntity> list = repository.findAll();
		model.addAttribute("list", list);
		return "csv";
	}	
	
	
	// ----- download ----- ----- ----- ----- ----- ----- ----- ----- ----- -----
	
	// 2026/06/16
	@GetMapping("/download1")
	public String download1(
			@ModelAttribute("form") Form form,
			Model model) {
		System.out.println("/download1");
		System.out.println(form);
//		List<DiaryEntity> list = form.getList();
		
		List<DiaryEntity> list = repository.findAll();
		
		LocalDateTime datetime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
		
		String dirpath = "C:\\Users\\watanabe-senri\\";
//		String dirpath = "C:\\Users\\watan\\";
		System.out.println(dirpath);
		String filename = "download" + "_" + datetime.format(formatter) + ".csv";
		System.out.println(filename);
		String filepath = dirpath + filename;
		System.out.println(filepath);
		
		Charset cs = Charset.forName("Shift_JIS");	// 文字セット
		
		String line;
		try (BufferedWriter bw = new BufferedWriter(
				new FileWriter(filepath, cs))) {	// 文字セット指定
			line = String.join(",",
					"id",
					"date",
					"title",
					"memo",
					"category",
					"type",
					"filename");
			bw.write(line);
			bw.newLine();	// 改行
			for (DiaryEntity entity : list) {
				line = String.join(",",
						entity.getId().toString(),
						entity.getDate().toString(),
						entity.getTitle(),
						entity.getMemo(),
						entity.getCategory(),
						entity.getType().toString(),
						entity.getFilename());
				bw.write(line);
				bw.newLine();
			}
		} catch (IOException e) {
			System.err.println(e);
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		return "redirect:/csv";
	}
	
	
	// ----- upload ----- ----- ----- ----- ----- ----- ----- ----- ----- -----
	
	// 2026/06/16
	@GetMapping("/upload1")
	public String upload1(
//			@RequestParam("file") MultipartFile file,
			Model model) {
		System.out.println("/upload1");
		
//		String filepath = "C:\\Users\\watanabe-senri\\" + file.getOriginalFilename();
		String filepath = "C:\\Users\\watanabe-senri\\" + "download_20261026093647.csv";
//		String filepath = "C:\\Users\\watan\\" + "download_20260616224044.csv";
		
		System.out.println(filepath);
		
		Charset cs = Charset.forName("Shift_JIS");	// 文字セット
		
		List<DiaryEntity> list = new ArrayList<>();
		String line;
		try (BufferedReader br = new BufferedReader(
				new FileReader(filepath, cs))) {
			line = br.readLine();	// 先頭行読み飛ばし
			while ((line = br.readLine()) != null) {
//				System.out.println(line);
				String[] strs = line.split(",");
				DiaryEntity entity = new DiaryEntity();
				
				if ("".equals(strs[0])) {
					strs[0] = "0";	// 仮置き
				}
				Integer id = Integer.parseInt(strs[0]);
				Optional<DiaryEntity> opt = repository.findById(id);
				if (opt.isPresent()) { 		// idが存在していれば、上書き
					entity.setId(id);
				} else {					// idが存在していなければ、新規作成
					entity.setId(null);
				}
				entity.setDate(toLocalDate(strs[1]));
				entity.setTitle(strs[2]);
				entity.setMemo(strs[3]);
				entity.setCategory(strs[4]);
				entity.setType(strs[5].replace("[", "").replace("]", "").split(","));
				entity.setFilename(strs[6]);
				System.out.println(entity);
				repository.saveAndFlush(entity);
				list.add(entity);
			}
		} catch (IOException e) {
			System.err.println(e);
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		
		System.out.println(list);
		return "redirect:/csv";
	}
	
	// String→LocalDate
	public LocalDate toLocalDate(String str) {
		LocalDate date = null;
		try {
			date = LocalDate.parse(str);
		} catch(DateTimeParseException e) {
			e.printStackTrace();
		}
		return date;
	}
	
	// 2026/06/16
	@PostMapping("/upload2")
	public String upload2(
				@RequestParam("file") MultipartFile file,
				Model model) {
		System.out.println("/upload2");
		
		String filepath = "C:\\Users\\watanabe-senri\\" + file.getOriginalFilename();
//		String filepath = "C:\\Users\\watan\\" + file.getOriginalFilename();
		System.out.println(filepath);
		
		Charset cs = Charset.forName("Shift_JIS");	// 文字セット
		
		List<DiaryEntity> list = new ArrayList<>();
		String line;
		try (BufferedReader br = new BufferedReader(
				new FileReader(filepath, cs))) {
			line = br.readLine();	// 先頭行読み飛ばし
			while ((line = br.readLine()) != null) {
//					System.out.println(line);
				String[] strs = line.split(",");
				DiaryEntity entity = new DiaryEntity();
				
				if ("".equals(strs[0])) {
					strs[0] = "0";	// 仮置き
				}
				Integer id = Integer.parseInt(strs[0]);
				Optional<DiaryEntity> opt = repository.findById(id);
				if (opt.isPresent()) { 		// idが存在していれば、上書き
					entity.setId(id);
				} else {					// idが存在していなければ、新規作成
					entity.setId(null);
				}
				entity.setDate(toLocalDate(strs[1]));
				entity.setTitle(strs[2]);
				entity.setMemo(strs[3]);
				entity.setCategory(strs[4]);
				entity.setType(strs[5].replace("[", "").replace("]", "").split(","));
				entity.setFilename(strs[6]);
				System.out.println(entity);
				repository.saveAndFlush(entity);
				list.add(entity);
			}
		} catch (IOException e) {
			System.err.println(e);
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		
		System.out.println(list);
		return "redirect:/csv";
	}
	
	
//	アップロード
//	https://qiita.com/zabo/items/b6707c75ed9115f00fbf
	
//	ダウンロード
//	https://qiita.com/crml1206/items/7166d3ffbd5cf2152eec
//	https://nainaistar.hatenablog.com/entry/2021/10/21/120000
//	https://zenn.dev/ooooomu/articles/f746649b30813d
//	https://qiita.com/macha_7/items/11dc7a65b96d00c4cbcb
	
//	https://qiita.com/t-iguchi/items/75c879fc9a3423c511cb
//	https://qiita.com/yo1000/items/050c5c47daabf7a10e10
//	https://qiita.com/t-iguchi/items/75c879fc9a3423c511cb
		
//		https://zenn.dev/ooooomu/articles/f746649b30813d
	
	
//	https://ittrip.xyz/java/spring-boot-file-upload-download
	
//	https://memory-lovers.blog/entry/2016/05/20/005755
	
//	https://saycon.co.jp/portal-for-newcomer/spring-boot/webtips/pdf-csv-download
//	プロジェクトのルートに
	
	
	
	
	// ----- カレンダー表示 ----- ----- ----- ----- ----- ----- ----- ----- ----- -----
	
//	カレンダー表示
//	https://qiita.com/ktr0203ozeken/items/a68de6be6442f60e2737
//	https://mr-hamasan.com/homemade-mogumogu-4/
	
	// 2026/06/16
	@GetMapping("/calendar")
	public String calendar(
			@RequestParam(value = "year", required = false) Integer year,
			@RequestParam(value = "month", required = false) Integer month,
			Model model) {
		System.out.println("/calendar");
		
//		Integer year = 2026;
//		Integer month = 6;
		
		//nullの場合は,今日の年月を取得
		LocalDate today = LocalDate.now();
		int currentYear = (year != null) ? year : today.getYear();
		int currentMonth = (month != null) ? month : today.getMonthValue();
		
		/**  前月・次月計算用 */
		//指定された年月の初日を取得
		LocalDate firstDay = LocalDate.of(currentYear, currentMonth, 1);
		LocalDate lastDay = firstDay.plusMonths(1).minusDays(1);
		System.out.println("firstDay=" + firstDay);
		System.out.println("lastDay =" + lastDay);
		
		//指定された年月の前月・次月を取得
		LocalDate prevMonth = firstDay.minusMonths(1);
		LocalDate nextMonth = firstDay.plusMonths(1);
		
		//指定された年月の家計簿を取得
		List<DiaryEntity> list = repository.findByDateBetween(firstDay, lastDay);
		System.out.println(list);
		
		//月初めの曜日を取得し、日曜始まりへと変換
		int firstDayOfWeek = firstDay.getDayOfWeek().getValue();
		firstDayOfWeek = (firstDayOfWeek == 7) ? 0 : firstDayOfWeek;
		//その月が何日あるか計算
		int daysInMonth = firstDay.lengthOfMonth();
		
		model.addAttribute("year", currentYear);
		model.addAttribute("month", currentMonth);
		model.addAttribute("list", list);
		model.addAttribute("firstDayOfWeek", firstDayOfWeek);
		model.addAttribute("daysInMonth", daysInMonth);
		// 前月・次月の年月を渡す
		model.addAttribute("prevYear", prevMonth.getYear());
		model.addAttribute("prevMonth", prevMonth.getMonthValue());
		model.addAttribute("nextYear", nextMonth.getYear());
		model.addAttribute("nextMonth", nextMonth.getMonthValue());
		
		return "calendar";
	
	}
	
	
	
	// ----- パターン４ ----- ----- ----- ----- ----- ----- ----- ----- ----- -----
	// 2026/06/18,20
	
	@Autowired
	DishRepository dishRepository;
	
	@Autowired
	KondateRepository kondateRepository;
	
	@GetMapping("/dishList")
	public String dishList(Model model) {
		System.out.println("/dishList");
		List<DishEntity> list = dishRepository.findAll();
		model.addAttribute("list", list);
		return "dishList";
	}
	
	@GetMapping("/dishWrite")
	public String dishWrite(Model model) {
		System.out.println("/dishWrite");
		model.addAttribute("dish", new DishEntity());
		return "dishWrite";
	}
	
	@PostMapping("/dishInsert")
	@Transactional(readOnly = false)
	public String dishInsert(
			@ModelAttribute("dish") DishEntity dish,
			Model model) {
		System.out.println("/dishInsert");
		System.out.println(dish);
		dishRepository.saveAndFlush(dish);
		return "redirect:/dishList";
	}
	
	@GetMapping("/dishDetail/{dishid}")
	public String dishDetail(
			@PathVariable Integer dishid,
			Model model) {
		System.out.println("/dishDetail/{dishid}");
		System.out.println("dishid=" + dishid);
		Optional<DishEntity> data = dishRepository.findById(dishid);
		model.addAttribute("dish", data.get()); // Optionalから取得
		return "dishDetail";
	}
	
	@GetMapping("/dishUpdate/{dishid}")
	public String dishUpdate(
			@PathVariable Integer dishid,
			Model model) {
		System.out.println("/dishUpdate/{dishid}");
		System.out.println("dishid=" + dishid);
		Optional<DishEntity> data = dishRepository.findById(dishid);
		DishEntity dish = data.get();
		model.addAttribute("dish", dish);
		return "dishUpdate";
	}
	
	@GetMapping("/dishDelete/{dishid}")
	public String dishDelete(
			@PathVariable Integer dishid,
			Model model) {
		System.out.println("/dishDelete/{dishid}");
		System.out.println("dishid=" + dishid);
		Optional<DishEntity> data = dishRepository.findById(dishid);
		model.addAttribute("dish", data.get());
		return "dishDelete";
	}
	
	@PostMapping("/dishRemove")
	@Transactional(readOnly = false)
	public String dishRemove(
			@ModelAttribute("dish") DishEntity dish) {
		System.out.println("/dishRemove");
		int dishid = dish.getDishid();
		dishRepository.deleteById(dishid);
		return "redirect:/dishList";
	}
	
	@GetMapping("/kondateList")
	public String kondateList(Model model) {
		List<KondateEntity> list = kondateRepository.findAll();
		model.addAttribute("list", list);
		return "kondateList";
	}

	@GetMapping("/kondateWrite")
	public String kondateWrite(Model model) {
		System.out.println("/kondateWrite");
		model.addAttribute("kondate", new KondateEntity());
		return "kondateWrite";
	}
	
	@PostMapping("/kondateInsert")
	@Transactional(readOnly = false)
	public String kondateInsert(
			@ModelAttribute("kondate") KondateEntity kondate,
			Model model) {
		System.out.println("/kondateInsert");
		System.out.println(kondate);
		if (kondate.getKondateid() == null) {
			List<Integer> list = List.of();
			kondate.setDishids(list);
		}
		kondateRepository.saveAndFlush(kondate);
		return "redirect:/kondateList";
	}
	
	@GetMapping("/kondateDetail/{kondateid}")
	public String kondateDetail(
			@PathVariable Integer kondateid,
			Model model) {
		System.out.println("/kondateDetail/{kondateid}");
		System.out.println("kondateid=" + kondateid);
		Optional<KondateEntity> data = kondateRepository.findById(kondateid);
		KondateEntity kondate = data.get();
		model.addAttribute("kondate", kondate);
		List<Integer> dishids = kondate.getDishids();
		System.out.println("dishids=" + dishids);
//		List<DishEntity> list = dishRepository.findByDishidIn(dishids);
		// dishidsの順に表示するようにする
		List<DishEntity> list = new ArrayList<>();
		if (dishids != null) {
			for (Integer dishid : dishids) {
				Optional<DishEntity> data2 = dishRepository.findById(dishid);
				DishEntity dish = data2.get();
				list.add(dish);
			}
		}
		System.out.println(list);
		model.addAttribute("list", list);
		return "kondateDetail";
	}
	
	@GetMapping("/kondateUpdate/{kondateid}")
	public String kondateUpdate(
			@PathVariable Integer kondateid,
			Model model) {
		System.out.println("/kondateUpdate/{kondateid}");
		System.out.println("kondateid=" + kondateid);
		Optional<KondateEntity> data = kondateRepository.findById(kondateid);
		KondateEntity kondate = data.get();
		model.addAttribute("kondate", kondate);
		List<Integer> dishids = kondate.getDishids();
		System.out.println("dishids=" + dishids);
//		List<DishEntity> list = dishRepository.findByDishidIn(dishids);
		// dishidsの順に表示するようにする
		List<DishEntity> list = new ArrayList<>();
		if (dishids != null) {
			for (Integer dishid : dishids) {
				Optional<DishEntity> data2 = dishRepository.findById(dishid);
				DishEntity dish = data2.get();
				list.add(dish);
			}
		}
		System.out.println(list);
		model.addAttribute("list", list);
		return "kondateUpdate";
	}
	
	@GetMapping("/kondateDelete/{kondateid}")
	public String kondateDelete(
			@PathVariable Integer kondateid,
			Model model) {
		System.out.println("/deleteKondate/{kondateid}");
		System.out.println("kondateid=" + kondateid);
		Optional<KondateEntity> data = kondateRepository.findById(kondateid);
		model.addAttribute("kondate", data.get());
		return "kondateDelete";
	}
	
	@PostMapping("/kondateRemove")
	@Transactional(readOnly = false)
	public String kondateRemove(
			@ModelAttribute("kondate") KondateEntity kondate) {
		System.out.println("/kondateRemove");
		int kondateid = kondate.getKondateid();
		kondateRepository.deleteById(kondateid);
		return "redirect:/kondateList";
	}
	
	@GetMapping("/dishSelect/{kondateid}")
	public String dishSelect(
			@PathVariable Integer kondateid,
			Model model) {
		System.out.println("/selectDish/{kondateid}");
		System.out.println("kondateid=" + kondateid);
		model.addAttribute("kondateid", kondateid);
		List<DishEntity> list = dishRepository.findAll();
		model.addAttribute("list", list);
		return "dishList";  
	}
	
	@GetMapping("/dishAdd/{dishid}/{kondateid}")
	@Transactional(readOnly = false)
	public String dishAdd(
			@PathVariable Integer dishid,
			@PathVariable Integer kondateid,
			Model model) {
		System.out.println("/dishAdd/{dishid}/{kondateid}");
		System.out.println("dishid=" + dishid);
		System.out.println("kondateid=" + kondateid);
		Optional<KondateEntity> data = kondateRepository.findById(kondateid);
		KondateEntity kondate = data.get();
		List<Integer> dishids = kondate.getDishids();
		System.out.println(dishids);
		if (!dishids.contains(dishid)) {
			dishids.add(dishid);
		}
		System.out.println(dishids);
		kondate.setDishids(dishids);
		kondateRepository.saveAndFlush(kondate);
//		List<DishEntity> list = dishRepository.findByDishidIn(dishids);
//		model.addAttribute("kondate", kondate);
//		System.out.println(list);
//		model.addAttribute("list", list);
//		return "kondateUpdate";
		return "redirect:/kondateUpdate/" + kondateid;
	}
	
	@GetMapping("/dishCancel/{dishid}/{kondateid}")
	@Transactional(readOnly = false)
	public String dishCancel(
			@PathVariable Integer dishid,
			@PathVariable Integer kondateid,
			Model model) {
		System.out.println("/cancelDish/{dishid}/{kondateid}");
		System.out.println("dishid=" + dishid);
		System.out.println("kondateid=" + kondateid);
		Optional<KondateEntity> data = kondateRepository.findById(kondateid);
		KondateEntity kondate = data.get();
		List<Integer> dishids = kondate.getDishids();
		System.out.println(dishids);
		if (dishids.contains(dishid)) {
			dishids.remove(dishid);
		}
		System.out.println(dishids);
		kondate.setDishids(dishids);
		kondateRepository.saveAndFlush(kondate);
//		List<DishEntity> list = dishRepository.findByDishidIn(dishids);
//		model.addAttribute("kondate", kondate);
//		System.out.println(list);
//		model.addAttribute("list", list);
//		return "kondateUpdate";  
		return "redirect:/kondateUpdate/" + kondateid;
	}
	
	
	
	// ----- パターン３ ----- ----- ----- ----- ----- ----- ----- ----- ----- -----
	// 2026/06/20
	
	@Autowired
	TeamRepository teamRepository;
	
	@Autowired
	PlayerRepository playerRepository;
	
	@GetMapping("/teamList")
	public String teamList(Model model) {
		System.out.println("/teamList");
		List<TeamEntity> list = teamRepository.findAll();
		model.addAttribute("list", list);
		return "teamList";
	}
	
	@GetMapping("/teamWrite")
	public String teamWrite(Model model) {
		System.out.println("/teamWrite");
		model.addAttribute("team", new TeamEntity());
		return "teamWrite";
	}
	
	@PostMapping("/teamInsert")
	@Transactional(readOnly = false)
	public String teamInsert(
			@ModelAttribute("team") TeamEntity team,
			Model model) {
		System.out.println("/teamInsert");
		System.out.println(team);
		teamRepository.saveAndFlush(team);
		return "redirect:/teamList";
	}
	
	@GetMapping("/teamDetail/{teamid}")
	public String teamDetail(
			@PathVariable Integer teamid,
			Model model) {
		System.out.println("/teamDetail/{teamid}");
		System.out.println("teamid=" + teamid);
		Optional<TeamEntity> data = teamRepository.findById(teamid);
		model.addAttribute("team", data.get()); // Optionalから取得
		
		// player一覧表示用
		List<PlayerEntity> list = playerRepository.findByTeamid(teamid);
		model.addAttribute("list", list);
		return "teamDetail";
	}
	
	@GetMapping("/teamUpdate/{teamid}")
	public String teamUpdate(
			@PathVariable Integer teamid,
			Model model) {
		System.out.println("/teamUpdate/{teamid}");
		System.out.println("teamid=" + teamid);
		Optional<TeamEntity> data = teamRepository.findById(teamid);
		TeamEntity team = data.get();
		model.addAttribute("team", team);
		return "teamUpdate";
	}
	
	@GetMapping("/teamDelete/{teamid}")
	public String teamDelete(
			@PathVariable Integer teamid,
			Model model) {
		System.out.println("/dishDelete/{teamid}");
		System.out.println("teamid=" + teamid);
		Optional<TeamEntity> data = teamRepository.findById(teamid);
		model.addAttribute("team", data.get());
		return "teamDelete";
	}
	
	@PostMapping("/teamRemove")
	@Transactional(readOnly = false)
	public String teamRemove(
			@ModelAttribute("team") TeamEntity team) {
		System.out.println("/teamRemove");
		int teamid = team.getTeamid();
		teamRepository.deleteById(teamid);
		return "redirect:/teamList";
	}
	
	@GetMapping("/playerWrite/{teamid}")
	public String teamWrite(
			@PathVariable Integer teamid,
			Model model) {
		System.out.println("/playerWrite/{teamid}");
		System.out.println("teamid=" + teamid);
		PlayerEntity player = new PlayerEntity();
		player.setTeamid(teamid);
		System.out.println(player);
		model.addAttribute("player", player);
		return "playerWrite";
	}
	
	@PostMapping("/playerInsert")
	@Transactional(readOnly = false)
	public String playerInsert(
			@ModelAttribute("player") PlayerEntity player,
			Model model) {
		System.out.println("/playerInsert");
		System.out.println(player);
		playerRepository.saveAndFlush(player);
		return "redirect:/teamDetail/" + player.getTeamid();
	}
	
	@GetMapping("/playerDetail/{playerid}")
	public String playerDetail(
			@PathVariable Integer playerid,
			Model model) {
		System.out.println("/playerDetail/{playerid}");
		System.out.println("playerid=" + playerid);
		Optional<PlayerEntity> data = playerRepository.findById(playerid);
		model.addAttribute("player", data.get()); // Optionalから取得
		return "playerDetail";
	}
	
	@GetMapping("/playerUpdate/{playerid}")
	public String playerUpdate(
			@PathVariable Integer playerid,
			Model model) {
		System.out.println("/playerUpdate/{playerid}");
		System.out.println("playerid=" + playerid);
		Optional<PlayerEntity> data = playerRepository.findById(playerid);
		PlayerEntity player = data.get();
		model.addAttribute("player", player);
		return "playerUpdate";
	}
	
	@GetMapping("/playerDelete/{playerid}")
	public String playerDelete(
			@PathVariable Integer playerid,
			Model model) {
		System.out.println("/playerDelete/{playerid}");
		System.out.println("playerid=" + playerid);
		Optional<PlayerEntity> data = playerRepository.findById(playerid);
		model.addAttribute("player", data.get());
		return "playerDelete";
	}
	
	@PostMapping("/playerRemove")
	@Transactional(readOnly = false)
	public String playerRemove(
			@ModelAttribute("player") PlayerEntity player) {
		System.out.println("/playerRemove");
		int playerid = player.getPlayerid();
		playerRepository.deleteById(playerid);
		return "redirect:/teamDetail/" + player.getTeamid();
	}
	
	
	// ----- 値の受け渡し ----- ----- ----- ----- ----- ----- ----- ----- ----- -----
	// 2026/06/21
	@GetMapping("/delivery/{a}/{b}")
	public String delivery(
			@PathVariable Integer a,
			@PathVariable Integer b,
			Model model) {
		System.out.println("/delivery/{a}/{b}");
		System.out.println("a=" + a);
		System.out.println("b=" + b);
		model.addAttribute("a", a + 1);
		model.addAttribute("b", b + 1);
		return "delivery";
	}
	
	@GetMapping("/deliveryLink/{a}/{b}")
	public String deliveryLink(
			@PathVariable Integer a,
			@PathVariable Integer b,
			Model model) {
		System.out.println("/deliveryLink/{a}/{b}");
		System.out.println("a=" + a);
		System.out.println("b=" + b);
		model.addAttribute("a", a+1);
		model.addAttribute("a", b+1);
		return "top";
	}
	
	
}
