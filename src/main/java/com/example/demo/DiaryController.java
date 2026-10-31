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
		model.addAttribute("form", new DiaryEntity());
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
		
		if (file.isEmpty()) {	// 画像ファイル未選択
			System.out.println("画像ファイル：未選択");
		} else {
			System.out.println("file: " + file.getOriginalFilename());
		}
		
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
				Path path = Paths.get("src/main/resources/static/img/" + file.getOriginalFilename());
				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.getStackTrace();
			}
			
			entity.setFilename(file.getOriginalFilename());
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
	
	
	// 2026/06/16 ページング
//	@GetMapping("/paging")
//	public String paging(
//			@PageableDefault(size = 5) Pageable pageable,
//			Model model) {
//		System.out.println("/paging");
//		Page<DiaryEntity> list = repository.findAll(pageable);
//		model.addAttribute("list", list);
//		return "paging";
//	}
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
	
	
	// 2026/10/24 ソート（並び替え）
	@GetMapping("/sort")
	public String sort(
			Model model) {
		System.out.println("/sort");
		List<DiaryEntity> list = repository.findAll();
		model.addAttribute("list", list);
		model.addAttribute("jun", "");
		model.addAttribute("sortForm", new SortForm());
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
//		Sort sort = Sort.by(Sort.Direction.ASC, "id").and(Sort.by(Sort.Direction.ASC, "date"));
//		Sort sort = Sort.by(Sort.Direction.ASC, "date");
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
			@RequestParam("deleteIds") List<Integer> deleteIds,
			Model model) {
		System.out.println("/bulkdeletePost");
		System.out.println("deleteIds=" + deleteIds);
		for (Integer id : deleteIds) {
			repository.deleteById(id);
		}
//		List<DiaryEntity> list = repository.findAll();
//		model.addAttribute("list", list);
//		return "bulkdelete";
		return "redirect:/bulkdelete";
	}
	
	
	// 2026/06/16
	@GetMapping("/csv")
	public String csv(
			Model model) {
		System.out.println("/csv");
		
		List<DiaryEntity> list = repository.findAll();
		model.addAttribute("list", list);
		return "csv";
	}	
	
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
	
	
	// 2026/06/16
	@GetMapping("/upload1")
	public String upload1(
//			@RequestParam("file") MultipartFile file,
			Model model) {
		System.out.println("/upload1");
		
//		String filepath = "C:\\Users\\watanabe-senri\\" + file.getOriginalFilename();
		String filepath = "C:\\Users\\watanabe-senri\\" + "download_20260616141811.csv";
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
	
	
	
//	カレンダー表示
//	https://qiita.com/ktr0203ozeken/items/a68de6be6442f60e2737
//	https://mr-hamasan.com/homemade-mogumogu-4/
	
	
	
	
	
}
