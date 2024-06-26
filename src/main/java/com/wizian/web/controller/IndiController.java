package com.wizian.web.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.wizian.web.dto.BoardDTO;
import com.wizian.web.dto.UserDTO;
import com.wizian.web.service.BoardService;
import com.wizian.web.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class IndiController {

    @Autowired
    private BoardService boardService;
    @Autowired
    private UserService userService;

    @GetMapping("/indicoun")
    public String indiCoun(Model model) {

        List<Map<String, Object>> list = boardService.counProfile();
        model.addAttribute("counProfile", list);

        return "indicoun";
    }

    @GetMapping("/indiboard")
    public String indiboard(HttpSession session, @RequestParam(value = "bbsSn", required = false) Integer bbsSn,
                            @RequestParam(value = "page", defaultValue = "1") int page, Model model) {

        String userId = (String) session.getAttribute("userId");

        if (userId == null) {
            return "redirect:/login"; // 로그인 페이지로 리디렉션
        }

        if (bbsSn == null) {
            return "redirect:/indiboard?bbsSn=1&page=" + page; // 기본값으로 리디렉션
        }

        int pageSize = 10; // 페이지당 표시할 데이터 수
        int offset = (page - 1) * pageSize;
        List<BoardDTO> board = boardService.getBoards(bbsSn, userId, pageSize, offset);
        int totalItems = boardService.getTotalBoardCount(bbsSn, userId); // 전체 게시물 수
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);

        System.err.println(board);
        model.addAttribute("board", board);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", page);
        model.addAttribute("bbsSn", bbsSn);

        return "indiboard";
    }

    @GetMapping("/notice")
    public String notice(HttpSession session, @RequestParam(value = "bbsSn", required = false) Integer bbsSn,
                         @RequestParam(value = "page", defaultValue = "1") int page, Model model) {

        if (bbsSn == null) {
            return "redirect:/notice?bbsSn=2&page=" + page; // 기본값으로 리디렉션
        }

        try {
            int pageSize = 5; // 페이지당 표시할 데이터 수
            int offset = (page - 1) * pageSize;
            List<BoardDTO> board = boardService.getBoards(bbsSn, null, pageSize, offset); // userId를 null로 전달

            int totalItems = boardService.getTotalBoardCount(bbsSn, null); // userId를 null로 전달
            int totalPages = (int) Math.ceil((double) totalItems / pageSize);

            model.addAttribute("board", board);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("currentPage", page);
            model.addAttribute("bbsSn", bbsSn);

        } catch (Exception e) {
            model.addAttribute("error", "서버 오류 발생");
            e.printStackTrace(); // 전체 예외 스택 트레이스를 출력합니다.
            return "errorPage"; // 오류 페이지 뷰 이름
        }
        return "notice";
    }

    @GetMapping("/indiboard_apply")
    public String showConsultationForm(@RequestParam(name = "counselor", required = false) String counselor,
                                       Model model, HttpSession session) {

        String userId = (String) session.getAttribute("userId");
        if (userId == null)
            return "redirect:/login";

        UserDTO userInfo = userService.userInfo(userId);
        model.addAttribute("userInfo", userInfo);

        // DB에서 상담사 목록을 가져와서 모델에 추가
        List<String> counselors = boardService.getCounselors();
        model.addAttribute("counselors", counselors);

        if (counselor != null) {
            model.addAttribute("selectedCounselor", counselor);
        }
        return "indiboard_apply";
    }

    @PostMapping("/indiboard_apply")
    public String write(@RequestParam Map<String, Object> map, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login"; // 로그인 페이지로 리디렉션
        }
        map.put("pstg_ymd", LocalDate.now().toString()); // 현재 날짜를 게시일로 설정
        map.put("pst_comp", "미완료"); // 기본값을 '미완료'로 설정
        map.put("writer", userId); // 작성자를 세션에서 가져온 값으로 설정

        int result = boardService.write(map);
        return "redirect:/indiboard";
    }

    @GetMapping("/indiboard_detail")
    public String detail(@RequestParam("no") int no, Model model) {
        BoardDTO detail = boardService.detail(no);
        model.addAttribute("detail", detail);

        // 답글 목록 조회 추가
        List<BoardDTO> replies = boardService.getReplies(no);
        model.addAttribute("replies", replies);

        return "indiboard_detail";
    }

    @GetMapping("/indiboard_replies")
    @ResponseBody
    public ResponseEntity<List<BoardDTO>> getReplies(@RequestParam("no") int no) {
        List<BoardDTO> replies = boardService.getReplies(no);
        if (replies.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(replies);
        }
        return ResponseEntity.ok(replies);
    }

    @GetMapping("/notice_detail")
    public String notice_detail(@RequestParam("no") int no, Model model) {
        BoardDTO detail = boardService.detail(no);
        model.addAttribute("detail", detail);
        return "notice_detail";
    }

    @PostMapping("/submitReply")
    @ResponseBody
    public ResponseEntity<String> submitReply(@RequestBody Map<String, Object> map, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in");
        }
        map.put("writer", userId);
        map.put("pstg_ymd", LocalDate.now().toString());
        map.put("bbs_sn", 0); // BBS_SN 값을 1로 고정
        int result = boardService.submitReply(map);
        return ResponseEntity.ok("success");
    }

    @PostMapping("/submitReply2")
    @ResponseBody
    public ResponseEntity<String> submitReply2(@RequestBody Map<String, Object> data, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in");
        }

        data.put("writer", userId);
        data.put("pstg_ymd", LocalDate.now().toString());
        data.put("bbs_sn", 0); // BBS_SN 값을 0으로 고정

        // 디버깅을 위해 받은 데이터를 출력합니다.
        System.out.println("받은 데이터: " + data);

        int result = boardService.submitReply(data);
        if (result > 0) {
            return ResponseEntity.ok("success");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving reply");
        }
    }


    @PostMapping("/postDel")
    public String postDel(@RequestParam("no") int no) {
        return "redirect:/board";
    }
}
